package com.hotelos.reception.service;

import com.hotelos.common.dto.PageResponse;
import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.exception.ConcurrentRoutingMismatchException;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.GuestStayEntity;
import com.hotelos.reception.persistence.entity.ReservationEntity;
import com.hotelos.reception.persistence.entity.RoomEntity;
import com.hotelos.reception.persistence.entity.StayRoomHistoryEntity;
import com.hotelos.reception.persistence.repository.GuestStayRepository;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import com.hotelos.reception.persistence.repository.RoomRepository;
import com.hotelos.reception.persistence.repository.RoomServiceChargeRepository;
import com.hotelos.reception.persistence.repository.StayRoomHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StayService {
    private static final Logger log = LoggerFactory.getLogger(StayService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final Set<String> SORT_FIELDS = Set.of(
            "checkedInAt", "checkedOutAt", "checkInDate", "roomNumber", "createdAt"
    );

    private final StayTransactionService transactionService;
    private final GuestStayRepository guestStayRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final StayRoomHistoryRepository stayRoomHistoryRepository;
    private final RoomServiceChargeRepository roomServiceChargeRepository;
    private final BillingService billingService;

    public StayService(
            StayTransactionService transactionService,
            GuestStayRepository guestStayRepository,
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            StayRoomHistoryRepository stayRoomHistoryRepository,
            RoomServiceChargeRepository roomServiceChargeRepository,
            BillingService billingService
    ) {
        this.transactionService = transactionService;
        this.guestStayRepository = guestStayRepository;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.stayRoomHistoryRepository = stayRoomHistoryRepository;
        this.roomServiceChargeRepository = roomServiceChargeRepository;
        this.billingService = billingService;
    }

    public StayTransactionService.StayCreationResult checkInReservation(UUID reservationId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // Preliminary non-locking lookup
            ReservationEntity preliminary = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

            if (preliminary.getStatus() == ReservationStatus.CHECKED_IN) {
                GuestStayEntity existingStay = guestStayRepository.findByReservationId(reservationId)
                        .orElseThrow(() -> new IllegalStateException("Reservation marked CHECKED_IN but guest stay is missing"));
                List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(existingStay.getId());
                BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(existingStay.getId());
                BigDecimal total = billingService.calculateBill(existingStay, charges);
                return new StayTransactionService.StayCreationResult(
                        StayResponse.from(existingStay, history, charges, total), false
                );
            }

            ReservationRoutingSnapshot snapshot = new ReservationRoutingSnapshot(
                    preliminary.getId(),
                    preliminary.getRoomType(),
                    preliminary.getAssignedRoomNumber(),
                    preliminary.getStatus(),
                    preliminary.getArrivalDate(),
                    preliminary.getDepartureDate()
            );

            try {
                return transactionService.checkInReservation(reservationId, snapshot);
            } catch (ConcurrentRoutingMismatchException ex) {
                log.warn("Attempt {}/{}: Routing mismatch during check-in for reservation {}: {}",
                        attempt, MAX_ATTEMPTS, reservationId, ex.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    throw new ConflictException("CONCURRENT_STATE_CHANGE",
                            "Reservation routing was modified concurrently. Please retry operation.");
                }
                backoff(attempt);
            }
        }
        throw new ConflictException("CONCURRENT_STATE_CHANGE", "Max check-in retries exceeded");
    }

    @Transactional(readOnly = true)
    public StayResponse get(UUID stayId) {
        GuestStayEntity stay = guestStayRepository.findById(stayId)
                .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
        List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(stayId);
        BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(stayId);
        BigDecimal total = billingService.calculateBill(stay, charges);
        return StayResponse.from(stay, history, charges, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<StayResponse> list(int page, int size, String sort, Boolean activeOnly,
                                           UUID guestId, String roomNumber) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Sort parsedSort = parseSort(sort);

        Specification<GuestStayEntity> spec = Specification.where(null);
        if (Boolean.TRUE.equals(activeOnly)) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("checkedOutAt")));
        } else if (Boolean.FALSE.equals(activeOnly)) {
            spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("checkedOutAt")));
        }
        if (guestId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("guestId"), guestId));
        }
        if (roomNumber != null && !roomNumber.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("roomNumber"), roomNumber.trim()));
        }

        Page<GuestStayEntity> result = guestStayRepository.findAll(spec, PageRequest.of(safePage, safeSize, parsedSort));
        List<StayResponse> content = result.getContent().stream().map(stay -> {
            List<StayRoomHistoryEntity> history = stayRoomHistoryRepository.findByStayIdOrderByStartedAtAsc(stay.getId());
            BigDecimal charges = roomServiceChargeRepository.sumChargesByStayId(stay.getId());
            BigDecimal total = billingService.calculateBill(stay, charges);
            return StayResponse.from(stay, history, charges, total);
        }).toList();

        return PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public StayResponse extendStay(UUID stayId, ExtendStayRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            GuestStayEntity stay = guestStayRepository.findById(stayId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
            if (stay.isCheckedOut()) {
                throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Cannot extend a stay that is already checked out");
            }

            RoomEntity room = roomRepository.findById(stay.getRoomNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + stay.getRoomNumber()));

            StayRoutingSnapshot snapshot = new StayRoutingSnapshot(
                    stay.getId(),
                    stay.getRoomNumber(),
                    room.getType(),
                    stay.getCheckInDate(),
                    stay.getBookedNights(),
                    stay.getCheckInDate().plusDays(stay.getBookedNights()),
                    stay.getCheckedOutAt()
            );

            try {
                return transactionService.extendStay(stayId, request, snapshot);
            } catch (ConcurrentRoutingMismatchException ex) {
                log.warn("Attempt {}/{}: Routing mismatch during extend for stay {}: {}",
                        attempt, MAX_ATTEMPTS, stayId, ex.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    throw new ConflictException("CONCURRENT_STATE_CHANGE",
                            "Stay routing was modified concurrently. Please retry operation.");
                }
                backoff(attempt);
            }
        }
        throw new ConflictException("CONCURRENT_STATE_CHANGE", "Max extend retries exceeded");
    }

    public StayResponse changeRoom(UUID stayId, ChangeRoomRequest request) {
        String targetRoomNumber = request.targetRoomNumber().trim();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            GuestStayEntity stay = guestStayRepository.findById(stayId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
            if (stay.isCheckedOut()) {
                throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Cannot change room for a checked-out stay");
            }
            if (stay.getRoomNumber().equals(targetRoomNumber)) {
                throw new ConflictException("SAME_ROOM", "Target room must be different from current room");
            }

            RoomEntity currentRoom = roomRepository.findById(stay.getRoomNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Current room not found: " + stay.getRoomNumber()));
            RoomEntity targetRoom = roomRepository.findById(targetRoomNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Target room not found: " + targetRoomNumber));

            StayRoutingSnapshot snapshot = new StayRoutingSnapshot(
                    stay.getId(),
                    stay.getRoomNumber(),
                    currentRoom.getType(),
                    stay.getCheckInDate(),
                    stay.getBookedNights(),
                    stay.getCheckInDate().plusDays(stay.getBookedNights()),
                    stay.getCheckedOutAt()
            );

            try {
                return transactionService.changeRoom(stayId, request, snapshot, targetRoom.getType());
            } catch (ConcurrentRoutingMismatchException ex) {
                log.warn("Attempt {}/{}: Routing mismatch during change-room for stay {}: {}",
                        attempt, MAX_ATTEMPTS, stayId, ex.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    throw new ConflictException("CONCURRENT_STATE_CHANGE",
                            "Stay routing was modified concurrently. Please retry operation.");
                }
                backoff(attempt);
            }
        }
        throw new ConflictException("CONCURRENT_STATE_CHANGE", "Max change-room retries exceeded");
    }

    public CheckOutResponse checkOutStay(UUID stayId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            GuestStayEntity stay = guestStayRepository.findById(stayId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stay not found: " + stayId));
            if (stay.isCheckedOut()) {
                throw new ConflictException("STAY_ALREADY_CHECKED_OUT", "Stay " + stayId + " is already checked out");
            }

            StayRoutingSnapshot snapshot = new StayRoutingSnapshot(
                    stay.getId(),
                    stay.getRoomNumber(),
                    null,
                    stay.getCheckInDate(),
                    stay.getBookedNights(),
                    stay.getCheckInDate().plusDays(stay.getBookedNights()),
                    stay.getCheckedOutAt()
            );

            try {
                return transactionService.checkOutStay(stayId, snapshot);
            } catch (ConcurrentRoutingMismatchException ex) {
                log.warn("Attempt {}/{}: Routing mismatch during checkout for stay {}: {}",
                        attempt, MAX_ATTEMPTS, stayId, ex.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    throw new ConflictException("CONCURRENT_STATE_CHANGE",
                            "Stay routing was modified concurrently. Please retry operation.");
                }
                backoff(attempt);
            }
        }
        throw new ConflictException("CONCURRENT_STATE_CHANGE", "Max checkout retries exceeded");
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(attempt * 25L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", ie);
        }
    }

    private Sort parseSort(String sort) {
        String property = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", -1);
            if (parts.length > 2 || !SORT_FIELDS.contains(parts[0].trim())) {
                throw new HotelValidationException("Unsupported stay sort: " + sort);
            }
            property = parts[0].trim();
            if (parts.length == 2) {
                try {
                    direction = Sort.Direction.fromString(parts[1].trim());
                } catch (IllegalArgumentException ex) {
                    throw new HotelValidationException("Unsupported sort direction: " + parts[1]);
                }
            }
        }
        return Sort.by(direction, property);
    }
}
