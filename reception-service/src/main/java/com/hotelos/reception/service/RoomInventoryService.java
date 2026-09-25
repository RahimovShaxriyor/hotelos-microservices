package com.hotelos.reception.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomEngineeringStatusChangedPayload;
import com.hotelos.common.event.payload.RoomHousekeepingStatusChangedPayload;
import com.hotelos.common.event.payload.RoomOccupancyChangedPayload;
import com.hotelos.common.event.payload.RoomServiceChargePayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.reception.config.RabbitConfig;
import com.hotelos.reception.domain.*;
import com.hotelos.reception.dto.CheckInRequest;
import com.hotelos.reception.dto.CheckInResponse;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.inbox.ReceptionInboxService;
import com.hotelos.reception.outbox.ReceptionOutboxService;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.entity.RoomServiceChargeEntity;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.ReceptionInboxRepository;
import com.hotelos.reception.persistence.repository.ReceptionOutboxRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import com.hotelos.reception.persistence.repository.RoomServiceChargeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class RoomInventoryService {
    private static final Logger log = LoggerFactory.getLogger(RoomInventoryService.class);

    private final RoomRepository roomRepository;
    private final GuestStayRepository guestStayRepository;
    private final RoomServiceChargeRepository roomServiceChargeRepository;
    private final BillingService billingService;
    private final ReceptionOutboxService outboxService;
    private final ReceptionOutboxRepository outboxRepository;
    private final ReceptionInboxService inboxService;
    private final ReceptionInboxRepository inboxRepository;

    public RoomInventoryService(
            RoomRepository roomRepository,
            GuestStayRepository guestStayRepository,
            RoomServiceChargeRepository roomServiceChargeRepository,
            BillingService billingService,
            ReceptionOutboxService outboxService,
            ReceptionOutboxRepository outboxRepository,
            ReceptionInboxService inboxService,
            ReceptionInboxRepository inboxRepository
    ) {
        this.roomRepository = roomRepository;
        this.guestStayRepository = guestStayRepository;
        this.roomServiceChargeRepository = roomServiceChargeRepository;
        this.billingService = billingService;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
        this.inboxService = inboxService;
        this.inboxRepository = inboxRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        if (roomRepository.count() == 0) {
            log.info("Empty reception schema detected on startup. Initializing demo seed data...");
            resetAndSeed();
        } else {
            log.info("Reception database already initialized ({} rooms present). Preserving existing persistent state.",
                    roomRepository.count());
        }
    }

    @Transactional
    public Map<String, Object> resetAndSeed() {
        roomServiceChargeRepository.deleteAllInBatch();
        guestStayRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        inboxRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();

        seedRooms();
        seedExistingGuests();

        long roomCount = roomRepository.count();
        long activeGuestCount = guestStayRepository.findByCheckedOutAtIsNullOrderByRoomNumberAsc().size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Reception demo data has been reset");
        result.put("rooms", roomCount);
        result.put("activeGuests", activeGuestCount);
        return result;
    }

    @Transactional
    public Map<String, Object> seed() {
        return resetAndSeed();
    }

    private void seedRooms() {
        Instant now = Instant.now();
        List<RoomEntity> rooms = List.of(
                new RoomEntity("101", 1, RoomType.SINGLE, "LIFT", new BigDecimal("90.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(7200), false, null),
                new RoomEntity("102", 1, RoomType.DOUBLE, "STAIRS", new BigDecimal("130.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(3600), false, null),
                new RoomEntity("103", 1, RoomType.SUITE, "LIFT", new BigDecimal("240.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(5000), false, null),
                new RoomEntity("104", 1, RoomType.ACCESSIBLE, "LIFT", new BigDecimal("120.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(6000), false, null),
                new RoomEntity("115", 1, RoomType.SINGLE, "STAIRS", new BigDecimal("90.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(1000), false, null),
                new RoomEntity("201", 2, RoomType.SINGLE, "LIFT", new BigDecimal("95.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(8000), false, null),
                new RoomEntity("202", 2, RoomType.DOUBLE, "LIFT", new BigDecimal("140.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(9000), false, null),
                new RoomEntity("204", 2, RoomType.DOUBLE, "STAIRS", new BigDecimal("140.00"),
                        OccupancyStatus.OCCUPIED, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(9000), false, null),
                new RoomEntity("301", 3, RoomType.DOUBLE, "LIFT", new BigDecimal("150.00"),
                        OccupancyStatus.OCCUPIED, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(12000), false, null),
                new RoomEntity("302", 3, RoomType.DOUBLE, "STAIRS", new BigDecimal("150.00"),
                        OccupancyStatus.VACANT, HousekeepingStatus.CLEAN, EngineeringStatus.OPERATIONAL, now.minusSeconds(15000), false, null)
        );
        roomRepository.saveAll(rooms);
    }

    private void seedExistingGuests() {
        Instant now = Instant.now();
        List<GuestStayEntity> stays = List.of(
                new GuestStayEntity(UUID.randomUUID(), "204", "John Smith", LocalDate.now(), 2,
                        new BigDecimal("140.00"), now.minusSeconds(9000)),
                new GuestStayEntity(UUID.randomUUID(), "301", "Sara Lee", LocalDate.now(), 3,
                        new BigDecimal("150.00"), now.minusSeconds(12000))
        );
        guestStayRepository.saveAll(stays);
    }

    @Transactional(readOnly = true)
    public List<Room> getRooms() {
        return roomRepository.findAllByOrderByRoomNumberAsc().stream()
                .map(Room::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Room getRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        return roomRepository.findById(roomNumber)
                .map(Room::fromEntity)
                .orElseThrow(() -> new HotelValidationException("Invalid room number: " + roomNumber));
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(String roomType, Integer floor) {
        RoomType parsedType = (roomType != null && !roomType.isBlank()) ? parseRoomType(roomType) : null;
        return roomRepository.findAllByOrderByRoomNumberAsc().stream()
                .filter(RoomEntity::isSellable)
                .filter(r -> parsedType == null || r.getType() == parsedType)
                .filter(r -> floor == null || r.getFloor() == floor)
                .sorted(Comparator.comparing(RoomEntity::getCleanSince, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RoomEntity::getRoomNumber))
                .map(Room::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuestStay> getGuests() {
        return guestStayRepository.findByCheckedOutAtIsNullOrderByRoomNumberAsc().stream()
                .map(s -> GuestStay.fromEntity(s, roomServiceChargeRepository.sumChargesByStayId(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuestStay getGuestByRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        return guestStayRepository.findByRoomNumberAndCheckedOutAtIsNull(roomNumber)
                .map(s -> GuestStay.fromEntity(s, roomServiceChargeRepository.sumChargesByStayId(s.getId())))
                .orElseThrow(() -> new HotelValidationException("No active guest in room " + roomNumber));
    }

    @Transactional
    public GuestStay archiveGuest(String guestId) {
        UUID stayUuid;
        try {
            stayUuid = UUID.fromString(guestId);
        } catch (Exception e) {
            throw new HotelValidationException("Unknown guest stay ID: " + guestId);
        }

        GuestStayEntity stay = guestStayRepository.findById(stayUuid)
                .filter(s -> !s.isCheckedOut())
                .orElseThrow(() -> new HotelValidationException("Unknown guest stay ID: " + guestId));

        stay.setArchived(true);
        guestStayRepository.save(stay);

        BigDecimal rsCharges = roomServiceChargeRepository.sumChargesByStayId(stay.getId());
        return GuestStay.fromEntity(stay, rsCharges);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateBillForRoom(String roomNumber) {
        validateKnownRoom(roomNumber);
        GuestStayEntity stay = guestStayRepository.findByRoomNumberAndCheckedOutAtIsNull(roomNumber)
                .orElseThrow(() -> new HotelValidationException("No active guest stay for room " + roomNumber));

        BigDecimal rsCharges = roomServiceChargeRepository.sumChargesByStayId(stay.getId());
        BigDecimal total = billingService.calculateBill(stay, rsCharges);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomNumber", roomNumber);
        result.put("guestName", stay.getGuestName());
        result.put("bookedNights", stay.getBookedNights());
        result.put("roomServiceCharges", rsCharges);
        result.put("total", total);
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckInResponse checkIn(CheckInRequest request) {
        validateCheckIn(request);
        RoomType requestedType = parseRoomType(request.getRoomType());

        // Row-level locking candidate selection: selects best candidate with FOR UPDATE SKIP LOCKED
        Optional<RoomEntity> candidate = roomRepository.findAvailableRoomForUpdate(
                requestedType.name(),
                request.getPreferredFloor(),
                request.getProximityPreference()
        );

        if (candidate.isEmpty()) {
            throw new HotelValidationException("No rooms available for requested type");
        }

        RoomEntity selected = candidate.get();
        selected.occupy();
        roomRepository.save(selected);

        GuestStayEntity stay = new GuestStayEntity(
                UUID.randomUUID(),
                selected.getRoomNumber(),
                request.getGuestName().trim(),
                LocalDate.now(),
                request.getNights(),
                selected.getNightlyRate(),
                Instant.now()
        );
        guestStayRepository.save(stay);

        // Enqueue outbox event atomically within check-in transaction
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                selected.getRoomNumber(),
                null,
                new RoomOccupancyChangedPayload(selected.getRoomNumber(), selected.getOccupancyStatus().name(), Instant.now())
        );
        outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        log.info("Check-in committed for guest {} in room {}", stay.getGuestName(), selected.getRoomNumber());

        return new CheckInResponse(
                stay.getId().toString(),
                stay.getGuestName(),
                selected.getRoomNumber(),
                selected.getType().name(),
                selected.getOccupancyStatus(),
                "Guest checked in successfully"
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckOutResponse checkOut(String roomNumber) {
        validateKnownRoom(roomNumber);

        // Lock hierarchy: 1. RoomEntity, 2. GuestStayEntity
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(roomNumber)
                .orElseThrow(() -> new HotelValidationException("Invalid room number: " + roomNumber));

        GuestStayEntity stay = guestStayRepository.findActiveStayForUpdate(roomNumber)
                .orElseThrow(() -> new HotelValidationException("No active guest stay for room " + roomNumber));

        BigDecimal rsCharges = roomServiceChargeRepository.sumChargesByStayId(stay.getId());
        BigDecimal total = billingService.calculateBill(stay, rsCharges);

        stay.setCheckedOutAt(Instant.now());
        guestStayRepository.save(stay);

        // Check-out business operation correlation ID shared by related events and turnover lifecycle
        String correlationId = UUID.randomUUID().toString();

        // Atomically vacate occupancy and arm turnoverPending with this turnover correlation ID
        room.vacate(correlationId);
        roomRepository.save(room);

        Instant now = Instant.now();
        // Enqueue occupancy changed (VACANT) and room vacated events atomically within checkout transaction
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                roomNumber,
                correlationId,
                new RoomOccupancyChangedPayload(roomNumber, OccupancyStatus.VACANT.name(), now)
        );
        outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        EventEnvelope<RoomVacatedPayload> vacatedEvent = EventEnvelope.create(
                EventTypes.ROOM_VACATED,
                "reception-service",
                roomNumber,
                correlationId,
                new RoomVacatedPayload(roomNumber, now)
        );
        outboxService.enqueue(vacatedEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_VACATED);

        log.info("Checkout committed for room {} (stay {}), bill total {}", roomNumber, stay.getId(), total);

        return new CheckOutResponse(roomNumber, stay.getGuestName(), total, "Guest checked out and room vacated event published");
    }

    @RabbitListener(queues = RabbitConfig.RECEPTION_HOUSEKEEPING_STATUS_QUEUE)
    @Transactional
    public void onHousekeepingStatusChanged(EventEnvelope<RoomHousekeepingStatusChangedPayload> event) {
        ReceptionInboxService.InboxResult inboxResult = inboxService.registerEvent(event);
        if (inboxResult != ReceptionInboxService.InboxResult.PROCEED) {
            return;
        }

        RoomHousekeepingStatusChangedPayload payload = event.payload();
        String roomNumber = payload.roomNumber();

        Optional<RoomEntity> roomOpt = roomRepository.findByRoomNumberForUpdate(roomNumber);
        if (roomOpt.isEmpty()) {
            log.warn("Room {} not found for housekeeping status update", roomNumber);
            return;
        }

        RoomEntity room = roomOpt.get();
        HousekeepingStatus newStatus = HousekeepingStatus.valueOf(payload.housekeepingStatus());
        Instant changedAt = payload.changedAt() != null ? payload.changedAt() : Instant.now();

        if (room.isTurnoverPending()) {
            if (newStatus == HousekeepingStatus.CLEAN) {
                if (event.correlationId() != null && event.correlationId().equals(room.getTurnoverCorrelationId())) {
                    room.applyHousekeepingStatus(newStatus, changedAt, event.correlationId());
                    roomRepository.save(room);
                    log.info("Turnover pending cleared for room {} with matching correlationId {}", roomNumber, event.correlationId());
                } else {
                    log.warn("Ignoring stale CLEAN event for room {} with correlationId {}. Current turnover correlationId is {}",
                            roomNumber, event.correlationId(), room.getTurnoverCorrelationId());
                    // Stale CLEAN ignored completely: do NOT update housekeeping_status, do NOT update clean_since, do NOT clear turnoverPending!
                }
            } else {
                room.applyHousekeepingStatus(newStatus, changedAt, event.correlationId());
                roomRepository.save(room);
            }
        } else {
            room.applyHousekeepingStatus(newStatus, changedAt, event.correlationId());
            roomRepository.save(room);
        }
        // HARD ARCHITECTURAL RULE: Consumer projection MUST NOT republish!
    }

    @RabbitListener(queues = RabbitConfig.RECEPTION_ENGINEERING_STATUS_QUEUE)
    @Transactional
    public void onEngineeringStatusChanged(EventEnvelope<RoomEngineeringStatusChangedPayload> event) {
        ReceptionInboxService.InboxResult inboxResult = inboxService.registerEvent(event);
        if (inboxResult != ReceptionInboxService.InboxResult.PROCEED) {
            return;
        }

        RoomEngineeringStatusChangedPayload payload = event.payload();
        EngineeringStatus newStatus = EngineeringStatus.valueOf(payload.engineeringStatus());
        roomRepository.updateEngineeringStatus(payload.roomNumber(), newStatus);
        // HARD ARCHITECTURAL RULE: Consumer projection MUST NOT republish!
    }

    @RabbitListener(queues = RabbitConfig.RECEPTION_CHARGE_QUEUE)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void onRoomServiceCharge(EventEnvelope<RoomServiceChargePayload> event) {
        ReceptionInboxService.InboxResult inboxResult = inboxService.registerEvent(event);
        if (inboxResult != ReceptionInboxService.InboxResult.PROCEED) {
            return;
        }

        RoomServiceChargePayload payload = event.payload();
        String orderId = payload.orderId();
        if (orderId == null || orderId.isBlank()) {
            log.warn("Received room service charge without orderId, ignoring: {}", payload);
            return;
        }

        Instant chargeTime = payload.chargedAt() != null ? payload.chargedAt() : event.occurredAt();
        if (chargeTime == null) {
            chargeTime = Instant.now();
        }

        // Resolve historical stay active at the time the charge occurred (eliminating room reuse race)
        List<GuestStayEntity> candidateStays = guestStayRepository.findStaysForRoomAtTime(payload.roomNumber(), chargeTime);
        if (candidateStays.isEmpty()) {
            log.warn("No historical guest stay found for room {} at charge timestamp {}. Charge NOT applied for order {}. orderId remains unconsumed.",
                    payload.roomNumber(), chargeTime, orderId);
            return;
        }
        if (candidateStays.size() > 1) {
            log.error("Multiple overlapping stays found for room {} at charge timestamp {}. Data integrity error! Aborting charge for order {}.",
                    payload.roomNumber(), chargeTime, orderId);
            return;
        }

        GuestStayEntity stay = candidateStays.get(0);

        // Lock stay row to serialize against concurrent checkout bill calculation
        guestStayRepository.findByIdForUpdate(stay.getId());

        // Atomic PostgreSQL deduplication: INSERT ... ON CONFLICT (order_id) DO NOTHING
        UUID chargeId = UUID.randomUUID();
        int affected = roomServiceChargeRepository.insertChargeOnConflictDoNothing(
                chargeId,
                orderId,
                stay.getId(),
                payload.amount(),
                event.eventId(),
                chargeTime
        );

        if (affected == 1) {
            log.info("Applied room service charge of {} for order {} to stay {} (room {})",
                    payload.amount(), orderId, stay.getId(), payload.roomNumber());
        } else {
            // Conflict detected on unique order_id: fetch existing row to verify idempotency consistency
            Optional<RoomServiceChargeEntity> existingOpt = roomServiceChargeRepository.findByOrderId(orderId);
            if (existingOpt.isPresent()) {
                RoomServiceChargeEntity existing = existingOpt.get();
                if (existing.getStayId().equals(stay.getId()) && existing.getAmount().compareTo(payload.amount()) == 0) {
                    log.info("Duplicate room service charge ignored for orderId: {}", orderId);
                } else {
                    log.error("Duplicate orderId {} received with conflicting payload (data inconsistency). Existing stay={}, amount={}; incoming stay={}, amount={}. Charge rejected.",
                            orderId, existing.getStayId(), existing.getAmount(), stay.getId(), payload.amount());
                }
            } else {
                log.info("Duplicate room service charge ignored for orderId: {}", orderId);
            }
        }
    }

    private void validateCheckIn(CheckInRequest request) {
        if (request == null) {
            throw new HotelValidationException("Request body is required");
        }
        if (request.getGuestName() == null || request.getGuestName().isBlank()) {
            throw new HotelValidationException("Guest name is required");
        }
        if (request.getNights() <= 0) {
            throw new HotelValidationException("Nights must be greater than zero");
        }
        if (request.getRoomType() == null) {
            throw new HotelValidationException("Room type is required");
        }
        parseRoomType(request.getRoomType());
    }

    private RoomType parseRoomType(String roomType) {
        try {
            return RoomType.valueOf(roomType.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new HotelValidationException("Unsupported room type: " + roomType);
        }
    }

    private void validateKnownRoom(String roomNumber) {
        if (roomNumber == null || !roomRepository.existsById(roomNumber)) {
            throw new HotelValidationException("Invalid room number: " + roomNumber);
        }
    }
}
