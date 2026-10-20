package com.hotelos.reception.service;

import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.AvailabilityResponse;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.repository.RoomRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomInventoryAvailabilityService {
    private static final UUID NO_EXCLUDED_RESERVATION = new UUID(0L, 0L);

    private static final String DAILY_AVAILABILITY_SQL = """
            WITH days AS (
                SELECT generate_series(
                    CAST(:arrivalDate AS date),
                    CAST(:departureDate AS date) - 1,
                    INTERVAL '1 day'
                )::date AS business_date
            ),
            typed_rooms AS (
                SELECT r.room_number
                FROM reception.rooms r
                WHERE r.active = TRUE
                  AND r.room_type = :roomType
            ),
            out_of_order_rooms AS (
                SELECT d.business_date, r.room_number
                FROM days d
                CROSS JOIN reception.rooms r
                WHERE r.active = TRUE
                  AND r.room_type = :roomType
                  AND r.engineering_status = 'OUT_OF_ORDER'
            ),
            current_stay_rooms AS (
                SELECT d.business_date, s.room_number
                FROM days d
                JOIN reception.guest_stays s
                  ON s.checked_out_at IS NULL
                 AND s.check_in_date <= d.business_date
                 AND GREATEST(s.check_in_date + s.booked_nights, CAST(:hotelTodayPlusOne AS date)) > d.business_date
                JOIN reception.rooms r
                  ON r.room_number = s.room_number
                 AND r.room_type = :roomType
            ),
            reservation_consumers AS (
                SELECT d.business_date, res.id, res.assigned_room_number
                FROM days d
                JOIN reception.reservations res
                  ON res.status = 'CONFIRMED'
                 AND res.room_type = :roomType
                 AND res.id <> CAST(:excludedReservationId AS uuid)
                 AND res.arrival_date <= d.business_date
                 AND res.departure_date > d.business_date
            ),
            assigned_reservation_rooms AS (
                SELECT business_date, assigned_room_number AS room_number
                FROM reservation_consumers
                WHERE assigned_room_number IS NOT NULL
            ),
            blocked_physical_rooms AS (
                SELECT business_date, room_number FROM out_of_order_rooms
                UNION
                SELECT business_date, room_number FROM current_stay_rooms
                UNION
                SELECT business_date, room_number FROM assigned_reservation_rooms
            )
            SELECT
                d.business_date,
                (SELECT COUNT(*) FROM typed_rooms) AS total_physical_inventory,
                (SELECT COUNT(*) FROM out_of_order_rooms o WHERE o.business_date = d.business_date) AS permanently_unsellable_inventory,
                (SELECT COUNT(DISTINCT s.room_number) FROM current_stay_rooms s WHERE s.business_date = d.business_date) AS current_stay_rooms,
                (SELECT COUNT(*) FROM reservation_consumers rc WHERE rc.business_date = d.business_date) AS reservations_consuming_capacity,
                (SELECT COUNT(*) FROM blocked_physical_rooms b WHERE b.business_date = d.business_date) AS blocked_physical_rooms,
                (SELECT COUNT(*) FROM reservation_consumers ur WHERE ur.business_date = d.business_date AND ur.assigned_room_number IS NULL) AS unassigned_reservations
            FROM days d
            ORDER BY d.business_date
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RoomRepository roomRepository;

    public RoomInventoryAvailabilityService(NamedParameterJdbcTemplate jdbcTemplate, RoomRepository roomRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.roomRepository = roomRepository;
    }

    public void lockRoomTypes(Collection<RoomType> roomTypes) {
        List<String> names = roomTypes.stream().map(Enum::name).distinct().sorted().toList();
        if (names.isEmpty()) return;
        List<String> locked = jdbcTemplate.queryForList("""
                SELECT room_type
                FROM reception.room_type_inventory_locks
                WHERE room_type IN (:roomTypes)
                ORDER BY room_type
                FOR UPDATE
                """, new MapSqlParameterSource("roomTypes", names), String.class);
        if (locked.size() != names.size()) {
            throw new IllegalStateException("Inventory lock row is missing for room type");
        }
    }

    public AvailabilityResponse availability(RoomType roomType, LocalDate arrivalDate, LocalDate departureDate,
                                             LocalDate hotelToday, UUID excludedReservationId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("roomType", roomType.name())
                .addValue("arrivalDate", arrivalDate)
                .addValue("departureDate", departureDate)
                .addValue("hotelTodayPlusOne", hotelToday.plusDays(1))
                .addValue("excludedReservationId", excludedReservationId == null ? NO_EXCLUDED_RESERVATION : excludedReservationId);

        List<AvailabilityResponse.DailyAvailability> daily = jdbcTemplate.query(
                DAILY_AVAILABILITY_SQL,
                parameters,
                (rs, rowNum) -> {
                    int total = rs.getInt("total_physical_inventory");
                    int blocked = rs.getInt("blocked_physical_rooms");
                    int unassigned = rs.getInt("unassigned_reservations");
                    return new AvailabilityResponse.DailyAvailability(
                            rs.getObject("business_date", LocalDate.class),
                            total,
                            rs.getInt("permanently_unsellable_inventory"),
                            rs.getInt("current_stay_rooms"),
                            rs.getInt("reservations_consuming_capacity"),
                            blocked,
                            unassigned,
                            Math.max(0, total - blocked - unassigned)
                    );
                }
        );
        int available = daily.stream()
                .map(AvailabilityResponse.DailyAvailability::availableInventory)
                .min(Comparator.naturalOrder())
                .orElse(0);
        return new AvailabilityResponse(roomType, arrivalDate, departureDate, available, daily);
    }

    public void assertCapacity(RoomType roomType, LocalDate arrivalDate, LocalDate departureDate,
                               LocalDate hotelToday, UUID excludedReservationId) {
        if (availability(roomType, arrivalDate, departureDate, hotelToday, excludedReservationId).availableInventory() < 1) {
            throw new ConflictException("NO_INVENTORY", "No inventory is available for the requested room type and dates");
        }
    }

    public RoomEntity lockAndValidateAssignedRoom(String roomNumber, RoomType roomType, LocalDate arrivalDate,
                                                  LocalDate departureDate, LocalDate hotelToday,
                                                  UUID excludedReservationId) {
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(roomNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomNumber));
        if (!room.isActive()) {
            throw new ConflictException("ROOM_INACTIVE", "Assigned room " + roomNumber + " is inactive");
        }
        if (room.getType() != roomType) {
            throw new ConflictException("ROOM_TYPE_MISMATCH", "Assigned room does not match reservation room type");
        }
        if (room.getEngineeringStatus() == EngineeringStatus.OUT_OF_ORDER) {
            throw new ConflictException("ROOM_OUT_OF_ORDER", "Assigned room is currently out of order");
        }

        MapSqlParameterSource parameters = overlapParameters(roomNumber, arrivalDate, departureDate, hotelToday, excludedReservationId);
        Boolean reservationOverlap = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM reception.reservations r
                    WHERE r.status = 'CONFIRMED'
                      AND r.assigned_room_number = :roomNumber
                      AND r.id <> CAST(:excludedReservationId AS uuid)
                      AND r.arrival_date < :departureDate
                      AND r.departure_date > :arrivalDate
                )
                """, parameters, Boolean.class);
        Boolean stayOverlap = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM reception.guest_stays s
                    WHERE s.checked_out_at IS NULL
                      AND s.room_number = :roomNumber
                      AND s.check_in_date < :departureDate
                      AND GREATEST(s.check_in_date + s.booked_nights, CAST(:hotelTodayPlusOne AS date)) > :arrivalDate
                )
                """, parameters, Boolean.class);
        if (Boolean.TRUE.equals(reservationOverlap) || Boolean.TRUE.equals(stayOverlap)) {
            throw new ConflictException("ROOM_ALREADY_ALLOCATED", "Assigned room is already allocated for the requested dates");
        }
        return room;
    }

    public Optional<RoomEntity> findReadyRoomForWalkIn(RoomType roomType, LocalDate arrivalDate, LocalDate departureDate,
                                                       Integer preferredFloor, String proximityPreference) {
        return roomRepository.findReservationAwareAvailableRoomForUpdate(
                roomType.name(), arrivalDate, departureDate, preferredFloor, proximityPreference
        );
    }

    public boolean hasConflictingConfirmedReservation(String roomNumber, LocalDate fromDate, LocalDate toDate,
                                                      UUID excludedReservationId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("roomNumber", roomNumber)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate)
                .addValue("excludedReservationId", excludedReservationId == null ? NO_EXCLUDED_RESERVATION : excludedReservationId);
        Boolean conflict = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM reception.reservations r
                    WHERE r.status = 'CONFIRMED'
                      AND r.assigned_room_number = :roomNumber
                      AND r.id <> CAST(:excludedReservationId AS uuid)
                      AND r.arrival_date < :toDate
                      AND r.departure_date > :fromDate
                )
                """, parameters, Boolean.class);
        return Boolean.TRUE.equals(conflict);
    }

    private MapSqlParameterSource overlapParameters(String roomNumber, LocalDate arrivalDate, LocalDate departureDate,
                                                     LocalDate hotelToday, UUID excludedReservationId) {
        return new MapSqlParameterSource()
                .addValue("roomNumber", roomNumber)
                .addValue("arrivalDate", arrivalDate)
                .addValue("departureDate", departureDate)
                .addValue("hotelTodayPlusOne", hotelToday.plusDays(1))
                .addValue("excludedReservationId", excludedReservationId == null ? NO_EXCLUDED_RESERVATION : excludedReservationId);
    }
}
