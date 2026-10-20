package com.hotelos.reception.service;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomOccupancyChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.HousekeepingStatus;
import com.hotelos.reception.domain.OccupancyStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.CreateRoomRequest;
import com.hotelos.reception.dto.RoomAdminResponse;
import com.hotelos.reception.dto.RoomReconcileResponse;
import com.hotelos.reception.dto.UpdateRoomRequest;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.outbox.ReceptionOutboxService;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomAdminService {

    private static final Logger log = LoggerFactory.getLogger(RoomAdminService.class);

    private final RoomRepository roomRepository;
    private final GuestStayRepository guestStayRepository;
    private final ReservationRepository reservationRepository;
    private final RoomInventoryAvailabilityService availabilityService;
    private final ReceptionOutboxService outboxService;
    private final Clock hotelClock;

    public RoomAdminService(
            RoomRepository roomRepository,
            GuestStayRepository guestStayRepository,
            ReservationRepository reservationRepository,
            RoomInventoryAvailabilityService availabilityService,
            ReceptionOutboxService outboxService,
            Clock hotelClock
    ) {
        this.roomRepository = roomRepository;
        this.guestStayRepository = guestStayRepository;
        this.reservationRepository = reservationRepository;
        this.availabilityService = availabilityService;
        this.outboxService = outboxService;
        this.hotelClock = hotelClock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomAdminResponse createRoom(CreateRoomRequest request) {
        String normalizedRoom = validateAndNormalizeRoomNumber(request.roomNumber());

        if (request.floor() == null || request.floor() <= 0) {
            throw new HotelValidationException("Floor must be greater than 0");
        }
        if (request.roomType() == null) {
            throw new HotelValidationException("Room type is required");
        }
        if (request.proximity() == null || request.proximity().isBlank()) {
            throw new HotelValidationException("Proximity is required");
        }
        String normalizedProximity = request.proximity().trim();
        if (normalizedProximity.length() > 32) {
            throw new HotelValidationException("Proximity cannot exceed 32 characters");
        }
        if (request.nightlyRate() == null || request.nightlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new HotelValidationException("Nightly rate must be greater than 0");
        }
        String currency = "UZS";
        if (request.currency() != null && !request.currency().isBlank()) {
            String trimmedCurrency = request.currency().trim().toUpperCase();
            if (!"UZS".equals(trimmedCurrency)) {
                throw new HotelValidationException("Currency must be UZS");
            }
            currency = trimmedCurrency;
        }

        // 1. Acquire transaction-scoped PostgreSQL advisory lock on hashed room number
        roomRepository.acquireRoomCreationAdvisoryLock(normalizedRoom);

        // 2. Authoritative pre-insert existence check
        if (roomRepository.existsById(normalizedRoom)) {
            throw new ConflictException("ROOM_ALREADY_EXISTS", "Room " + normalizedRoom + " already exists");
        }

        // 3. Persist new room entity: active = false, VACANT, DIRTY, OPERATIONAL
        Instant now = Instant.now(hotelClock);
        RoomEntity room = new RoomEntity(
                normalizedRoom,
                request.floor(),
                request.roomType(),
                normalizedProximity,
                request.nightlyRate(),
                currency,
                OccupancyStatus.VACANT,
                HousekeepingStatus.DIRTY,
                EngineeringStatus.OPERATIONAL,
                null,
                false,
                null,
                now,
                now,
                false
        );

        RoomEntity saved = roomRepository.save(room);
        log.info("Room created: {} (type={}, floor={}, rate={}, active=false)",
                saved.getRoomNumber(), saved.getType(), saved.getFloor(), saved.getNightlyRate());
        return RoomAdminResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomAdminResponse> listRooms(
            int page,
            int size,
            Boolean active,
            RoomType roomType,
            Integer floor,
            String search
    ) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(100, Math.max(1, size));

        Specification<RoomEntity> spec = Specification.where(null);
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (roomType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), roomType));
        }
        if (floor != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("floor"), floor));
        }
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.upper(root.get("roomNumber")), term));
        }

        Page<RoomEntity> roomPage = roomRepository.findAll(
                spec,
                PageRequest.of(validatedPage, validatedSize, Sort.by("roomNumber").ascending())
        );

        List<RoomAdminResponse> content = roomPage.getContent().stream()
                .map(RoomAdminResponse::from)
                .toList();

        return PageResponse.of(content, roomPage.getNumber(), roomPage.getSize(), roomPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RoomAdminResponse getRoom(String roomNumber) {
        String normalized = validateAndNormalizeRoomNumber(roomNumber);
        return roomRepository.findById(normalized)
                .map(RoomAdminResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomAdminResponse updateRoom(String roomNumber, UpdateRoomRequest request) {
        String normalized = validateAndNormalizeRoomNumber(roomNumber);

        RoomEntity preliminary = roomRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        availabilityService.lockRoomTypes(List.of(preliminary.getType()));
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        if (request.floor() != null) {
            if (request.floor() <= 0) {
                throw new HotelValidationException("Floor must be greater than 0");
            }
            room.setFloor(request.floor());
        }
        if (request.proximity() != null && !request.proximity().isBlank()) {
            String prox = request.proximity().trim();
            if (prox.length() > 32) {
                throw new HotelValidationException("Proximity cannot exceed 32 characters");
            }
            room.setProximity(prox);
        }
        if (request.nightlyRate() != null) {
            if (request.nightlyRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new HotelValidationException("Nightly rate must be greater than 0");
            }
            room.setNightlyRate(request.nightlyRate());
        }

        RoomEntity saved = roomRepository.save(room);
        log.info("Room updated: {} (floor={}, proximity={}, rate={})",
                saved.getRoomNumber(), saved.getFloor(), saved.getProximity(), saved.getNightlyRate());
        return RoomAdminResponse.from(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomAdminResponse activateRoom(String roomNumber) {
        String normalized = validateAndNormalizeRoomNumber(roomNumber);

        RoomEntity preliminary = roomRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        availabilityService.lockRoomTypes(List.of(preliminary.getType()));
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        if (room.isActive()) {
            throw new ConflictException("ROOM_ALREADY_ACTIVE", "Room " + normalized + " is already active");
        }

        room.setActive(true);
        RoomEntity saved = roomRepository.save(room);
        log.info("Room activated: {}", saved.getRoomNumber());
        return RoomAdminResponse.from(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomAdminResponse deactivateRoom(String roomNumber) {
        String normalized = validateAndNormalizeRoomNumber(roomNumber);

        RoomEntity preliminary = roomRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        availabilityService.lockRoomTypes(List.of(preliminary.getType()));
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        // Invariant 1: Must currently be active
        if (!room.isActive()) {
            throw new ConflictException("ROOM_ALREADY_INACTIVE", "Room " + normalized + " is already inactive");
        }

        // Invariant 2: Zero active stays
        if (guestStayRepository.findByRoomNumberAndCheckedOutAtIsNull(normalized).isPresent()) {
            throw new ConflictException("ROOM_HAS_ACTIVE_STAY", "Room " + normalized + " has an active guest stay");
        }

        // Invariant 3: Zero future confirmed reservations
        LocalDate today = LocalDate.now(hotelClock);
        if (reservationRepository.hasFutureConfirmedReservation(normalized, today)) {
            throw new ConflictException("ROOM_HAS_FUTURE_RESERVATION",
                    "Room " + normalized + " is assigned to a future confirmed reservation");
        }

        // Invariant 4: Zero in-flight turnover
        if (room.isTurnoverPending()) {
            throw new ConflictException("ROOM_TURNOVER_IN_PROGRESS",
                    "Room " + normalized + " has a turnover in progress");
        }

        room.setActive(false);
        RoomEntity saved = roomRepository.save(room);
        log.info("Room deactivated: {}", saved.getRoomNumber());
        return RoomAdminResponse.from(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomReconcileResponse reconcileOccupancy(String roomNumber) {
        String normalized = validateAndNormalizeRoomNumber(roomNumber);

        RoomEntity preliminary = roomRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        availabilityService.lockRoomTypes(List.of(preliminary.getType()));
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + normalized));

        Optional<GuestStayEntity> activeStayOpt = guestStayRepository.findByRoomNumberAndCheckedOutAtIsNull(normalized);
        OccupancyStatus previousStatus = room.getOccupancyStatus();
        Instant now = Instant.now(hotelClock);
        boolean changed = false;
        String reason;

        if (activeStayOpt.isPresent()) {
            if (room.getOccupancyStatus() == OccupancyStatus.OCCUPIED) {
                changed = false;
                reason = "ACTIVE_STAY_MATCHES_OCCUPIED";
            } else {
                changed = true;
                reason = "ACTIVE_STAY_EXISTS_TRANSITIONED_TO_OCCUPIED";
                room.occupy(now);
                roomRepository.save(room);

                EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                        EventTypes.ROOM_OCCUPANCY_CHANGED,
                        "reception-service",
                        room.getRoomNumber(),
                        null,
                        new RoomOccupancyChangedPayload(room.getRoomNumber(), OccupancyStatus.OCCUPIED.name(), now)
                );
                outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);
                log.info("Reconciliation changed room {} from VACANT to OCCUPIED", normalized);
            }
        } else {
            if (room.getOccupancyStatus() == OccupancyStatus.VACANT) {
                changed = false;
                reason = "NO_ACTIVE_STAY_MATCHES_VACANT";
            } else {
                changed = true;
                reason = "NO_ACTIVE_STAY_TRANSITIONED_TO_VACANT_AND_TURNOVER";
                String correlationId = UUID.randomUUID().toString();
                room.vacate(correlationId, now);
                roomRepository.save(room);

                EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                        EventTypes.ROOM_OCCUPANCY_CHANGED,
                        "reception-service",
                        room.getRoomNumber(),
                        correlationId,
                        new RoomOccupancyChangedPayload(room.getRoomNumber(), OccupancyStatus.VACANT.name(), now)
                );
                outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

                EventEnvelope<RoomVacatedPayload> vacatedEvent = EventEnvelope.create(
                        EventTypes.ROOM_VACATED,
                        "reception-service",
                        room.getRoomNumber(),
                        correlationId,
                        new RoomVacatedPayload(room.getRoomNumber(), now)
                );
                outboxService.enqueue(vacatedEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_VACATED);
                log.info("Reconciliation changed room {} from OCCUPIED to VACANT with turnover correlationId {}",
                        normalized, correlationId);
            }
        }

        return new RoomReconcileResponse(
                room.getRoomNumber(),
                previousStatus,
                room.getOccupancyStatus(),
                changed,
                reason,
                now
        );
    }

    private String validateAndNormalizeRoomNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        String normalized = raw.trim().toUpperCase();
        if (normalized.length() > 16) {
            throw new HotelValidationException("Room number cannot exceed 16 characters");
        }
        return normalized;
    }
}
