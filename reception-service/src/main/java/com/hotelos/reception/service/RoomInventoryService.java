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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.payload.RoomServiceChargePayloadV2;
import com.hotelos.reception.config.RabbitConfig;
import com.hotelos.reception.domain.*;
import com.hotelos.reception.dto.CheckInRequest;
import com.hotelos.reception.dto.CheckInResponse;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.inbox.ReceptionInboxService;
import com.hotelos.reception.outbox.ReceptionOutboxService;
import com.hotelos.reception.persistence.entity.GuestEntity;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.entity.RoomServiceChargeEntity;
import com.hotelos.reception.persistence.repository.FolioEntryRepository;
import com.hotelos.reception.persistence.repository.FolioRepository;
import com.hotelos.reception.persistence.repository.GuestRepository;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.ReceptionInboxRepository;
import com.hotelos.reception.persistence.repository.ReceptionOutboxRepository;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import com.hotelos.reception.persistence.repository.RoomServiceChargeRepository;
import com.hotelos.reception.persistence.repository.StayRoomHistoryRepository;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
public class RoomInventoryService {
    private static final Logger log = LoggerFactory.getLogger(RoomInventoryService.class);

    private final RoomRepository roomRepository;
    private final GuestStayRepository guestStayRepository;
    private final GuestRepository guestRepository;
    private final RoomServiceChargeRepository roomServiceChargeRepository;
    private final ReservationRepository reservationRepository;
    private final RoomInventoryAvailabilityService availabilityService;
    private final Clock hotelClock;
    private final BillingService billingService;
    private final ReceptionOutboxService outboxService;
    private final ReceptionOutboxRepository outboxRepository;
    private final ReceptionInboxService inboxService;
    private final ReceptionInboxRepository inboxRepository;
    private final StayRoomHistoryRepository stayRoomHistoryRepository;
    private final StayService stayService;
    private final FolioTransactionService folioTransactionService;
    private final FolioRepository folioRepository;
    private final FolioEntryRepository folioEntryRepository;
    private final ObjectMapper objectMapper;

    public RoomInventoryService(
            RoomRepository roomRepository,
            GuestStayRepository guestStayRepository,
            GuestRepository guestRepository,
            RoomServiceChargeRepository roomServiceChargeRepository,
            ReservationRepository reservationRepository,
            RoomInventoryAvailabilityService availabilityService,
            Clock hotelClock,
            BillingService billingService,
            ReceptionOutboxService outboxService,
            ReceptionOutboxRepository outboxRepository,
            ReceptionInboxService inboxService,
            ReceptionInboxRepository inboxRepository,
            StayRoomHistoryRepository stayRoomHistoryRepository,
            StayService stayService,
            FolioTransactionService folioTransactionService,
            FolioRepository folioRepository,
            FolioEntryRepository folioEntryRepository,
            ObjectMapper objectMapper
    ) {
        this.roomRepository = roomRepository;
        this.guestStayRepository = guestStayRepository;
        this.guestRepository = guestRepository;
        this.roomServiceChargeRepository = roomServiceChargeRepository;
        this.reservationRepository = reservationRepository;
        this.availabilityService = availabilityService;
        this.hotelClock = hotelClock;
        this.billingService = billingService;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
        this.inboxService = inboxService;
        this.inboxRepository = inboxRepository;
        this.stayRoomHistoryRepository = stayRoomHistoryRepository;
        this.stayService = stayService;
        this.folioTransactionService = folioTransactionService;
        this.folioRepository = folioRepository;
        this.folioEntryRepository = folioEntryRepository;
        this.objectMapper = objectMapper;
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
        folioEntryRepository.deleteAllInBatch();
        folioRepository.deleteAllInBatch();
        roomServiceChargeRepository.deleteAllInBatch();
        stayRoomHistoryRepository.deleteAllInBatch();
        guestStayRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
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
        LocalDate hotelToday = LocalDate.now(hotelClock);
        GuestEntity guest1 = new GuestEntity(UUID.randomUUID(), "John", "Smith", "john.smith@example.com", "+1-555-0101", false, "Seeded demo guest");
        GuestEntity guest2 = new GuestEntity(UUID.randomUUID(), "Sara", "Lee", "sara.lee@example.com", "+1-555-0102", true, "Seeded demo VIP guest");
        guestRepository.saveAll(List.of(guest1, guest2));

        List<GuestStayEntity> stays = List.of(
                new GuestStayEntity(UUID.randomUUID(), guest1.getId(), "204", "John Smith", "UZS", hotelToday, 2,
                        new BigDecimal("140.00"), now.minusSeconds(9000)),
                new GuestStayEntity(UUID.randomUUID(), guest2.getId(), "301", "Sara Lee", "UZS", hotelToday, 3,
                        new BigDecimal("150.00"), now.minusSeconds(12000))
        );
        guestStayRepository.saveAll(stays);

        for (GuestStayEntity s : stays) {
            stayRoomHistoryRepository.save(new com.hotelos.reception.persistence.entity.StayRoomHistoryEntity(
                    UUID.randomUUID(), s.getId(), s.getRoomNumber(), s.getCheckedInAt(), null, "INITIAL_CHECKIN", s.getCheckedInAt()
            ));
            folioTransactionService.createInitialFolio(s);
        }
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
        result.put("currency", stay.getCurrency());
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckInResponse checkIn(CheckInRequest request) {
        validateCheckIn(request);
        RoomType requestedType = parseRoomType(request.getRoomType());

        LocalDate hotelToday = LocalDate.now(hotelClock);
        LocalDate stayDepartureDate = hotelToday.plusDays(request.getNights());
        availabilityService.lockRoomTypes(List.of(requestedType));
        availabilityService.assertCapacity(requestedType, hotelToday, stayDepartureDate, hotelToday, null);

        UUID guestId;
        String guestName;
        if (request.getGuestId() != null) {
            GuestEntity guest = guestRepository.findById(request.getGuestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with ID: " + request.getGuestId()));
            guestId = guest.getId();
            guestName = (guest.getFirstName() + " " + guest.getLastName()).trim();
        } else {
            String fullName = request.getGuestName().trim();
            String firstName;
            String lastName;
            int firstSpace = fullName.indexOf(' ');
            if (firstSpace > 0) {
                firstName = fullName.substring(0, firstSpace).trim();
                lastName = fullName.substring(firstSpace + 1).trim();
                if (lastName.isBlank()) {
                    lastName = "Guest";
                }
            } else {
                firstName = fullName.isBlank() ? "Guest" : fullName;
                lastName = "Guest";
            }
            GuestEntity newGuest = new GuestEntity(UUID.randomUUID(), firstName, lastName, null, null, false, "Auto-created on check-in");
            guestRepository.save(newGuest);
            guestId = newGuest.getId();
            guestName = fullName;
        }

        Optional<RoomEntity> candidate = availabilityService.findReadyRoomForWalkIn(
                requestedType,
                hotelToday,
                stayDepartureDate,
                request.getPreferredFloor(),
                request.getProximityPreference()
        );

        if (candidate.isEmpty()) {
            throw new ConflictException("NO_ROOM_READY", "Capacity exists, but no physical room is ready for check-in");
        }

        RoomEntity selected = candidate.get();
        if (!selected.isSellable()) {
            throw new ConflictException("NO_ROOM_READY", "Selected room is no longer ready for check-in");
        }
        Instant checkedInAt = Instant.now();
        selected.occupy(checkedInAt);
        roomRepository.save(selected);

        GuestStayEntity stay = new GuestStayEntity(
                UUID.randomUUID(),
                guestId,
                selected.getRoomNumber(),
                guestName,
                selected.getCurrency(),
                hotelToday,
                request.getNights(),
                selected.getNightlyRate(),
                checkedInAt
        );
        guestStayRepository.save(stay);

        stayRoomHistoryRepository.save(new com.hotelos.reception.persistence.entity.StayRoomHistoryEntity(
                UUID.randomUUID(),
                stay.getId(),
                selected.getRoomNumber(),
                stay.getCheckedInAt(),
                null,
                "INITIAL_CHECKIN",
                stay.getCheckedInAt()
        ));

        // Create initial Folio and ROOM_CHARGE entry
        folioTransactionService.createInitialFolio(stay);

        // Enqueue outbox event atomically within check-in transaction
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                selected.getRoomNumber(),
                null,
                new RoomOccupancyChangedPayload(selected.getRoomNumber(), selected.getOccupancyStatus().name(), checkedInAt)
        );
        outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        log.info("Check-in committed for guest {} in room {}", stay.getGuestName(), selected.getRoomNumber());

        return new CheckInResponse(
                stay.getId().toString(),
                guestId.toString(),
                stay.getGuestName(),
                selected.getRoomNumber(),
                selected.getType().name(),
                selected.getOccupancyStatus(),
                stay.getCurrency(),
                "Guest checked in successfully"
        );
    }

    public CheckOutResponse checkOut(String roomNumber) {
        validateKnownRoom(roomNumber);
        GuestStayEntity stay = guestStayRepository.findByRoomNumberAndCheckedOutAtIsNull(roomNumber)
                .orElseThrow(() -> new HotelValidationException("No active guest stay for room " + roomNumber));
        return stayService.checkOutStay(stay.getId());
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
    @Transactional
    public void onRoomServiceCharge(EventEnvelope<JsonNode> event) {
        boolean isValidType = event != null && (EventTypes.ROOM_SERVICE_CHARGE.equals(event.eventType())
                || "ROOM_SERVICE_CHARGE_APPLIED".equals(event.eventType()));
        if (!isValidType) {
            log.warn("UNEXPECTED_EVENT_TYPE: expected {} but got {}",
                    EventTypes.ROOM_SERVICE_CHARGE, event != null ? event.eventType() : null);
            return;
        }

        switch (event.eventVersion()) {
            case 1 -> handleRoomServiceChargeV1(event);
            case 2 -> handleRoomServiceChargeV2(event);
            default -> {
                log.warn("UNSUPPORTED_EVENT_VERSION: Received unsupported version {} for eventId {}",
                        event.eventVersion(), event.eventId());
                // permanent reject: ACK, no mutation, no infinite requeue
            }
        }
    }

    private void handleRoomServiceChargeV1(EventEnvelope<JsonNode> event) {
        RoomServiceChargePayload payload;
        try {
            if (event.payload() == null || event.payload().isNull()) {
                throw new IllegalArgumentException("Payload is null");
            }
            payload = objectMapper.treeToValue(event.payload(), RoomServiceChargePayload.class);
            if (payload == null) {
                throw new IllegalArgumentException("Payload deserialized to null");
            }
            if (payload.orderId() == null || payload.orderId().isBlank()) {
                throw new IllegalArgumentException("missing orderId");
            }
            if (payload.roomNumber() == null || payload.roomNumber().isBlank()) {
                throw new IllegalArgumentException("missing roomNumber");
            }
            if (payload.amount() == null || payload.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("invalid amount");
            }
            if (payload.currency() == null || payload.currency().isBlank()) {
                throw new IllegalArgumentException("invalid currency");
            }
            if (payload.chargedAt() == null) {
                throw new IllegalArgumentException("missing chargedAt");
            }
        } catch (Exception ex) {
            log.warn("MALFORMED_EVENT_PAYLOAD (V1): {}", ex.getMessage());
            return; // ACK without mutation
        }

        ReceptionInboxService.InboxResult inboxResult = inboxService.registerEvent(event);
        if (inboxResult != ReceptionInboxService.InboxResult.PROCEED) {
            return;
        }

        FolioTransactionService.ChargeResult result = folioTransactionService.processRoomServiceCharge(
                payload, event.occurredAt(), event.eventId()
        );
        switch (result) {
            case APPLIED -> log.info("Applied room service charge of {} for order {}", payload.amount(), payload.orderId());
            case DUPLICATE -> log.info("Duplicate room service charge ignored for orderId: {}", payload.orderId());
            case UNRESOLVED -> log.warn("UNRESOLVED_STAY_AT_TIMESTAMP for room {} at {}", payload.roomNumber(), payload.chargedAt());
            case CURRENCY_MISMATCH -> log.warn("FOLIO_CURRENCY_MISMATCH for order {}", payload.orderId());
            case REJECTED_POST_CHECKOUT -> log.warn("REJECTED_POST_CHECKOUT for order {}", payload.orderId());
            case AMBIGUOUS_STAY_MATCH -> log.warn("AMBIGUOUS_STAY_MATCH for order {}", payload.orderId());
        }
    }

    private void handleRoomServiceChargeV2(EventEnvelope<JsonNode> event) {
        RoomServiceChargePayloadV2 payload;
        try {
            if (event.payload() == null || event.payload().isNull()) {
                throw new IllegalArgumentException("Payload is null");
            }
            payload = objectMapper.treeToValue(event.payload(), RoomServiceChargePayloadV2.class);
            if (payload == null) {
                throw new IllegalArgumentException("Payload deserialized to null");
            }
            if (payload.orderId() == null || payload.orderId().isBlank()) {
                throw new IllegalArgumentException("missing orderId");
            }
            if (payload.stayId() == null) {
                throw new IllegalArgumentException("missing stayId V2");
            }
            if (payload.amount() == null || payload.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("invalid amount");
            }
            if (payload.currency() == null || payload.currency().isBlank()) {
                throw new IllegalArgumentException("invalid currency");
            }
            if (payload.chargedAt() == null) {
                throw new IllegalArgumentException("missing chargedAt");
            }
        } catch (Exception ex) {
            log.warn("MALFORMED_EVENT_PAYLOAD (V2): {}", ex.getMessage());
            return; // ACK without mutation
        }

        ReceptionInboxService.InboxResult inboxResult = inboxService.registerEvent(event);
        if (inboxResult != ReceptionInboxService.InboxResult.PROCEED) {
            return;
        }

        FolioTransactionService.ChargeResult result = folioTransactionService.processRoomServiceChargeV2(
                payload, event.occurredAt(), event.eventId()
        );
        switch (result) {
            case APPLIED -> log.info("Applied room service charge V2 of {} for order {}", payload.amount(), payload.orderId());
            case DUPLICATE -> log.info("Duplicate room service charge V2 ignored for orderId: {}", payload.orderId());
            case UNRESOLVED -> log.warn("UNRESOLVED_STAY for stay {} order {}", payload.stayId(), payload.orderId());
            case CURRENCY_MISMATCH -> log.warn("FOLIO_CURRENCY_MISMATCH for order {}", payload.orderId());
            case REJECTED_POST_CHECKOUT -> log.warn("REJECTED_POST_CHECKOUT for order {}", payload.orderId());
            case AMBIGUOUS_STAY_MATCH -> log.warn("AMBIGUOUS_STAY_MATCH for order {}", payload.orderId());
        }
    }

    private void validateCheckIn(CheckInRequest request) {
        if (request == null) {
            throw new HotelValidationException("Request body is required");
        }
        boolean hasGuestId = request.getGuestId() != null;
        boolean hasGuestName = request.getGuestName() != null && !request.getGuestName().trim().isEmpty();
        if (hasGuestId && hasGuestName) {
            throw new HotelValidationException("Provide either guestId or guestName, not both");
        }
        if (!hasGuestId && !hasGuestName) {
            throw new HotelValidationException("Either guestId or guestName must be provided");
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
