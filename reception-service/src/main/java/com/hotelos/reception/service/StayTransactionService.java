package com.hotelos.reception.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomOccupancyChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.HousekeepingStatus;
import com.hotelos.reception.domain.OccupancyStatus;
import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.exception.ConcurrentRoutingMismatchException;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.outbox.ReceptionOutboxService;
import com.hotelos.reception.persistence.entity.GuestEntity;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.ReservationEntity;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.entity.StayRoomHistoryEntity;
import com.hotelos.reception.persistence.repository.GuestRepository;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import com.hotelos.reception.persistence.repository.RoomServiceChargeRepository;
import com.hotelos.reception.persistence.repository.StayRoomHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class StayTransactionService {
    private static final Logger log = LoggerFactory.getLogger(StayTransactionService.class);

    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final GuestStayRepository guestStayRepository;
    private final StayRoomHistoryRepository stayRoomHistoryRepository;
    private final RoomRepository roomRepository;
    private final RoomInventoryAvailabilityService availabilityService;
    private final RoomServiceChargeRepository roomServiceChargeRepository;
    private final BillingService billingService;
    private final FolioTransactionService folioTransactionService;
    private final ReceptionOutboxService outboxService;
    private final Clock hotelClock;

    public StayTransactionService(
            ReservationRepository reservationRepository,
            GuestRepository guestRepository,
            GuestStayRepository guestStayRepository,
            StayRoomHistoryRepository stayRoomHistoryRepository,
            RoomRepository roomRepository,
            RoomInventoryAvailabilityService availabilityService,
            RoomServiceChargeRepository roomServiceChargeRepository,
            BillingService billingService,
            FolioTransactionService folioTransactionService,
            ReceptionOutboxService outboxService,
            Clock hotelClock
    ) {
        this.reservationRepository = reservationRepository;
        this.guestRepository = guestRepository;
        this.guestStayRepository = guestStayRepository;
        this.stayRoomHistoryRepository = stayRoomHistoryRepository;
        this.roomRepository = roomRepository;
        this.availabilityService = availabilityService;
        this.roomServiceChargeRepository = roomServiceChargeRepository;
        this.billingService = billingService;
        this.folioTransactionService = folioTransactionService;
        this.outboxService = outboxService;
        this.hotelClock = hotelClock;
    }

    public record StayCreationResult(StayResponse response, boolean created) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public StayCreationResult checkInReservation(UUID reservationId, ReservationRoutingSnapshot snapshot) {
        // Level 1: RoomType Inventory Lock
        availabilityService.lockRoomTypes(List.of(snapshot.roomType()));

        // Level 2: Reservation Lock
        ReservationEntity reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            GuestStayEntity existingStay = guestStayRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new IllegalStateException("Reservation marked CHECKED_IN but guest stay is missing"));
            folioTransactionService.createInitialFolio(existingStay);
            List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(existingStay.getId());
            BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(existingStay.getId());
            BigDecimal total = billingService.calculateBill(existingStay, charges);
            return new StayCreationResult(StayResponse.from(existingStay, history, charges, total), false);
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("INVALID_RESERVATION_TRANSITION",
                    "Only CONFIRMED reservations can be checked in. Current status: " + reservation.getStatus());
        }

        // Revalidate routing fields against preliminary snapshot
        if (reservation.getRoomType() != snapshot.roomType()
                || !Objects.equals(reservation.getAssignedRoomNumber(), snapshot.assignedRoomNumber())) {
            throw new ConcurrentRoutingMismatchException("Reservation routing changed concurrently between lookup and lock");
        }

        LocalDate today = LocalDate.now(hotelClock);
        if (reservation.getArrivalDate().isAfter(today)) {
            throw new ConflictException("EARLY_CHECKIN_NOT_ALLOWED", "Check-in date cannot precede arrivalDate: " + reservation.getArrivalDate());
        }
        if (!today.isBefore(reservation.getDepartureDate())) {
            throw new ConflictException("PAST_DEPARTURE_CHECKIN", "Check-in cannot occur on or after departureDate: " + reservation.getDepartureDate());
        }

        // Level 3: Room Lock
        RoomEntity room;
        if (reservation.getAssignedRoomNumber() != null) {
            String roomNum = reservation.getAssignedRoomNumber();
            room = roomRepository.findByRoomNumberForUpdate(roomNum)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned room not found: " + roomNum));

            if (!room.isActive()
                    || room.getOccupancyStatus() != OccupancyStatus.VACANT
                    || room.getHousekeepingStatus() != HousekeepingStatus.CLEAN
                    || room.getEngineeringStatus() != EngineeringStatus.OPERATIONAL
                    || room.isTurnoverPending()) {
                throw new ConflictException("ASSIGNED_ROOM_NOT_READY",
                        "Assigned room " + roomNum + " is not ready for check-in");
            }
        } else {
            Optional<RoomEntity> candidate = availabilityService.findReadyRoomForWalkIn(
                    reservation.getRoomType(), today, reservation.getDepartureDate(), null, null
            );
            if (candidate.isEmpty()) {
                throw new ConflictException("NO_ROOM_READY",
                        "No ready room available for room type " + reservation.getRoomType());
            }
            room = candidate.get();
        }

        Instant now = Instant.now(hotelClock);
        room.occupy(now);
        roomRepository.save(room);

        reservation.markCheckedIn(now);
        if (reservation.getAssignedRoomNumber() == null) {
            reservation.setAssignedRoomNumber(room.getRoomNumber());
        }
        reservationRepository.save(reservation);

        GuestEntity guest = guestRepository.findById(reservation.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest profile not found: " + reservation.getGuestId()));
        String guestName = (guest.getFirstName() + " " + guest.getLastName()).trim();

        int bookedNights = (int) ChronoUnit.DAYS.between(today, reservation.getDepartureDate());
        if (bookedNights <= 0) bookedNights = 1;

        // Level 4: GuestStay (INSERT)
        GuestStayEntity stay = new GuestStayEntity(
                UUID.randomUUID(),
                guest.getId(),
                room.getRoomNumber(),
                guestName,
                reservation.getCurrency(),
                today,
                bookedNights,
                reservation.getNightlyRateSnapshot(),
                now
        );
        stay.setReservationId(reservation.getId());
        guestStayRepository.save(stay);

        // Level 5: StayRoomHistory (INSERT initial segment)
        StayRoomHistoryEntity history = new StayRoomHistoryEntity(
                UUID.randomUUID(),
                stay.getId(),
                room.getRoomNumber(),
                now,
                null,
                "INITIAL_CHECKIN",
                now
        );
        stayRoomHistoryRepository.save(history);

        // Level 6: Folio & Initial ROOM_CHARGE
        folioTransactionService.createInitialFolio(stay);

        // Enqueue outbox event atomically
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                room.getRoomNumber(),
                null,
                new RoomOccupancyChangedPayload(room.getRoomNumber(), OccupancyStatus.OCCUPIED.name(), now)
        );
        outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        BigDecimal total = billingService.calculateBill(stay, BigDecimal.ZERO);
        log.info("Checked in reservation {} into stay {} room {}, total bill {}", reservationId, stay.getId(), room.getRoomNumber(), total);
        return new StayCreationResult(StayResponse.from(stay, List.of(history), BigDecimal.ZERO, total), true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public StayResponse extendStay(UUID stayId, ExtendStayRequest request, StayRoutingSnapshot snapshot) {
        // Level 1: RoomType Inventory Lock
        availabilityService.lockRoomTypes(List.of(snapshot.currentRoomType()));

        // Level 3: Room Lock
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(snapshot.currentRoomNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + snapshot.currentRoomNumber()));

        // Level 4: Stay Lock
        GuestStayEntity stay = guestStayRepository.findByIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.isCheckedOut()) {
            throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Cannot extend a stay that is already checked out");
        }

        // Revalidate routing snapshot
        if (!stay.getRoomNumber().equals(snapshot.currentRoomNumber())) {
            throw new ConcurrentRoutingMismatchException("Stay room was modified concurrently");
        }
        LocalDate currentDepartureDate = stay.getCheckInDate().plusDays(stay.getBookedNights());
        if (!currentDepartureDate.equals(snapshot.departureDate())) {
            throw new ConcurrentRoutingMismatchException("Stay departure date was modified concurrently");
        }

        LocalDate newDepartureDate = request.newDepartureDate();
        if (newDepartureDate.equals(currentDepartureDate)) {
            List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(stayId);
            BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(stayId);
            BigDecimal total = billingService.calculateBill(stay, charges);
            return StayResponse.from(stay, history, charges, total);
        }

        if (newDepartureDate.isBefore(currentDepartureDate)) {
            throw new HotelValidationException(
                    "New departure date must be on or after current departure date: " + currentDepartureDate);
        }

        LocalDate today = LocalDate.now(hotelClock);
        // Category capacity check for [currentDepartureDate, newDepartureDate)
        availabilityService.assertCapacity(room.getType(), currentDepartureDate, newDepartureDate, today, null);

        // Physical room overlap check: ensure room is not assigned to any other CONFIRMED reservation
        if (availabilityService.hasConflictingConfirmedReservation(room.getRoomNumber(), currentDepartureDate, newDepartureDate, null)) {
            throw new ConflictException("ROOM_ALREADY_ALLOCATED",
                    "Room " + room.getRoomNumber() + " is already assigned to another reservation during the extension period");
        }

        int extraNights = (int) ChronoUnit.DAYS.between(currentDepartureDate, newDepartureDate);
        stay.setBookedNights(stay.getBookedNights() + extraNights);
        guestStayRepository.save(stay);

        // Level 6: Record extension charge
        folioTransactionService.recordExtensionCharge(stay.getId(), newDepartureDate, extraNights, stay.getNightlyRateAtCheckin(), Instant.now(hotelClock));

        List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(stayId);
        BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(stayId);
        BigDecimal total = billingService.calculateBill(stay, charges);

        log.info("Stay {} extended to {} (+{} nights)", stayId, newDepartureDate, extraNights);
        return StayResponse.from(stay, history, charges, total);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public StayResponse changeRoom(UUID stayId, ChangeRoomRequest request, StayRoutingSnapshot snapshot, RoomType targetRoomType) {
        // Level 1: RoomType Inventory Locks (ASC order)
        List<RoomType> sortedTypes = Stream.of(snapshot.currentRoomType(), targetRoomType)
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        availabilityService.lockRoomTypes(sortedTypes);

        // Level 3: Rooms Locks (ASC order)
        List<String> sortedRooms = Stream.of(snapshot.currentRoomNumber(), request.targetRoomNumber())
                .distinct()
                .sorted()
                .toList();
        for (String rNum : sortedRooms) {
            roomRepository.findByRoomNumberForUpdate(rNum)
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + rNum));
        }

        // Level 4: Stay Lock
        GuestStayEntity stay = guestStayRepository.findByIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.isCheckedOut()) {
            throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Cannot change room for a checked-out stay");
        }

        // Revalidate routing snapshot
        if (!stay.getRoomNumber().equals(snapshot.currentRoomNumber())) {
            throw new ConcurrentRoutingMismatchException("Stay room changed concurrently");
        }
        LocalDate currentDepartureDate = stay.getCheckInDate().plusDays(stay.getBookedNights());
        if (!currentDepartureDate.equals(snapshot.departureDate())) {
            throw new ConcurrentRoutingMismatchException("Stay departure date changed concurrently");
        }

        RoomEntity currentRoom = roomRepository.findByRoomNumberForUpdate(snapshot.currentRoomNumber()).orElseThrow();
        RoomEntity targetRoom = roomRepository.findByRoomNumberForUpdate(request.targetRoomNumber()).orElseThrow();

        // Physical validation of target room
        if (!targetRoom.isActive()
                || targetRoom.getOccupancyStatus() != OccupancyStatus.VACANT
                || targetRoom.getHousekeepingStatus() != HousekeepingStatus.CLEAN
                || targetRoom.getEngineeringStatus() != EngineeringStatus.OPERATIONAL
                || targetRoom.isTurnoverPending()) {
            throw new ConflictException("ROOM_NOT_READY",
                    "Target room " + request.targetRoomNumber() + " is not ready for check-in");
        }

        LocalDate today = LocalDate.now(hotelClock);
        // Specific target room overlap check
        if (availabilityService.hasConflictingConfirmedReservation(request.targetRoomNumber(), today, currentDepartureDate, null)) {
            throw new ConflictException("ROOM_ALREADY_ALLOCATED",
                    "Target room " + request.targetRoomNumber() + " is already assigned to another reservation during stay period");
        }

        // If cross-type, category capacity check on [today, currentDepartureDate)
        if (snapshot.currentRoomType() != targetRoomType) {
            availabilityService.assertCapacity(targetRoomType, today, currentDepartureDate, today, null);
        }

        Instant changedAt = Instant.now(hotelClock);

        // Level 5: Close current history segment and open new segment
        StayRoomHistoryEntity activeSegment = stayRoomHistoryRepository.findActiveSegmentForUpdate(stayId)
                .orElseThrow(() -> new IllegalStateException("Active room history segment missing for stay: " + stayId));
        activeSegment.setEndedAt(changedAt);
        stayRoomHistoryRepository.saveAndFlush(activeSegment);

        StayRoomHistoryEntity newSegment = new StayRoomHistoryEntity(
                UUID.randomUUID(),
                stayId,
                request.targetRoomNumber(),
                changedAt,
                null,
                request.reason(),
                changedAt
        );
        stayRoomHistoryRepository.save(newSegment);

        // Update stay current room (currency & nightly rate at check-in remain unchanged)
        stay.setRoomNumber(request.targetRoomNumber());
        guestStayRepository.save(stay);

        // Vacate current room with turnover
        String turnoverCorrelationId = UUID.randomUUID().toString();
        currentRoom.vacate(turnoverCorrelationId, changedAt);
        roomRepository.save(currentRoom);

        // Occupy target room
        targetRoom.occupy(changedAt);
        roomRepository.save(targetRoom);

        // Enqueue outbox events
        EventEnvelope<RoomOccupancyChangedPayload> oldOcc = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                currentRoom.getRoomNumber(),
                turnoverCorrelationId,
                new RoomOccupancyChangedPayload(currentRoom.getRoomNumber(), OccupancyStatus.VACANT.name(), changedAt)
        );
        outboxService.enqueue(oldOcc, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        EventEnvelope<RoomVacatedPayload> vacated = EventEnvelope.create(
                EventTypes.ROOM_VACATED,
                "reception-service",
                currentRoom.getRoomNumber(),
                turnoverCorrelationId,
                new RoomVacatedPayload(currentRoom.getRoomNumber(), changedAt)
        );
        outboxService.enqueue(vacated, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_VACATED);

        EventEnvelope<RoomOccupancyChangedPayload> newOcc = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                targetRoom.getRoomNumber(),
                null,
                new RoomOccupancyChangedPayload(targetRoom.getRoomNumber(), OccupancyStatus.OCCUPIED.name(), changedAt)
        );
        outboxService.enqueue(newOcc, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(stayId);
        BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(stayId);
        BigDecimal total = billingService.calculateBill(stay, charges);

        log.info("Stay {} changed room from {} to {}", stayId, currentRoom.getRoomNumber(), targetRoom.getRoomNumber());
        return StayResponse.from(stay, history, charges, total);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public CheckOutResponse checkOutStay(UUID stayId, StayRoutingSnapshot snapshot) {
        return checkOutStay(stayId, snapshot, null, java.util.Collections.emptyList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public CheckOutResponse checkOutStay(UUID stayId, StayRoutingSnapshot snapshot, UUID freezeId, List<RoomServiceChargeItem> charges) {
        // Level 3: Room Lock
        RoomEntity room = roomRepository.findByRoomNumberForUpdate(snapshot.currentRoomNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + snapshot.currentRoomNumber()));

        // Level 4: Stay Lock
        GuestStayEntity stay = guestStayRepository.findByIdForUpdate(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));

        if (stay.isCheckedOut()) {
            throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Stay " + stayId + " is already checked out");
        }

        if (!stay.getRoomNumber().equals(snapshot.currentRoomNumber())) {
            throw new ConcurrentRoutingMismatchException("Stay room changed concurrently");
        }

        Instant checkedOutAt = Instant.now(hotelClock);

        // Level 5: Close active history segment
        StayRoomHistoryEntity activeSegment = stayRoomHistoryRepository.findActiveSegmentForUpdate(stayId).orElse(null);
        if (activeSegment != null) {
            activeSegment.setEndedAt(checkedOutAt);
            stayRoomHistoryRepository.save(activeSegment);
        }

        // Reconcile snapshot charges if present
        if (charges != null && !charges.isEmpty()) {
            folioTransactionService.reconcileRoomServiceCharges(stayId, charges);
        }

        // Level 6: Early checkout credit & Close Folio
        LocalDate hotelToday = LocalDate.now(hotelClock);
        long daysBetween = ChronoUnit.DAYS.between(stay.getCheckInDate(), hotelToday);
        int actualNights = (int) Math.max(1, daysBetween);
        int unusedNights = stay.getBookedNights() - actualNights;
        if (unusedNights > 0) {
            folioTransactionService.recordEarlyCheckoutCredit(stay.getId(), unusedNights, stay.getNightlyRateAtCheckin(), checkedOutAt);
        }
        com.hotelos.reception.persistence.entity.FolioEntity closedFolio = folioTransactionService.closeFolio(stay.getId(), checkedOutAt);

        stay.setCheckedOutAt(checkedOutAt);
        guestStayRepository.save(stay);

        String turnoverCorrelationId = UUID.randomUUID().toString();
        room.vacate(turnoverCorrelationId, checkedOutAt);
        roomRepository.save(room);

        BigDecimal rsCharges = roomServiceChargeRepository.sumChargesByStayId(stay.getId());
        BigDecimal total = billingService.calculateBill(stay, rsCharges);

        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                room.getRoomNumber(),
                turnoverCorrelationId,
                new RoomOccupancyChangedPayload(room.getRoomNumber(), OccupancyStatus.VACANT.name(), checkedOutAt)
        );
        outboxService.enqueue(occupancyEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED);

        EventEnvelope<RoomVacatedPayload> vacatedEvent = EventEnvelope.create(
                EventTypes.ROOM_VACATED,
                "reception-service",
                room.getRoomNumber(),
                turnoverCorrelationId,
                new RoomVacatedPayload(room.getRoomNumber(), checkedOutAt)
        );
        outboxService.enqueue(vacatedEvent, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_VACATED);

        // Enqueue reception.stay.checked_out outbox event
        com.hotelos.common.event.payload.StayCheckedOutPayload checkedOutPayload =
                new com.hotelos.common.event.payload.StayCheckedOutPayload(
                        stayId,
                        closedFolio.getId(),
                        checkedOutAt,
                        freezeId
                );
        EventEnvelope<com.hotelos.common.event.payload.StayCheckedOutPayload> checkedOutEnvelope = EventEnvelope.create(
                EventTypes.STAY_CHECKED_OUT,
                "reception-service",
                stayId.toString(),
                null,
                checkedOutPayload
        );
        outboxService.enqueue(checkedOutEnvelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.STAY_CHECKED_OUT);

        log.info("Stay {} checked out for room {}, total bill {}, freezeId {}", stayId, room.getRoomNumber(), total, freezeId);
        return new CheckOutResponse(room.getRoomNumber(), stay.getGuestName(), total, stay.getCurrency(),
                "Guest checked out and room vacated event published");
    }
}
