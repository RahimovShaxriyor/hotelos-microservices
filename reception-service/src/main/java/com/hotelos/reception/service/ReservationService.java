package com.hotelos.reception.service;

import com.hotelos.common.currency.CurrencyValidator;
import com.hotelos.common.dto.PageResponse;
import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.ReservationEntity;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReservationService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "arrivalDate", "departureDate", "createdAt", "updatedAt", "status", "roomType"
    );

    private final ReservationRepository reservationRepository;
    private final ReservationTransactionService transactionService;
    private final ReservationFingerprintService fingerprintService;
    private final RoomInventoryAvailabilityService availabilityService;
    private final Clock hotelClock;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationTransactionService transactionService,
                              ReservationFingerprintService fingerprintService,
                              RoomInventoryAvailabilityService availabilityService,
                              Clock hotelClock) {
        this.reservationRepository = reservationRepository;
        this.transactionService = transactionService;
        this.fingerprintService = fingerprintService;
        this.availabilityService = availabilityService;
        this.hotelClock = hotelClock;
    }

    public ReservationCreationResult create(CreateReservationRequest request, String rawIdempotencyKey) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        String currency = CurrencyValidator.validateAndNormalize(request.currency());
        String assignedRoom = normalizeOptionalRoomNumber(request.assignedRoomNumber());
        String fingerprint = fingerprintService.fingerprint(request, currency, assignedRoom);
        try {
            ReservationTransactionService.CreationOutcome outcome = transactionService.create(
                    request, idempotencyKey, fingerprint, currency, assignedRoom
            );
            return new ReservationCreationResult(ReservationResponse.from(outcome.reservation()), outcome.created());
        } catch (DataIntegrityViolationException ex) {
            ReservationEntity winner = reservationRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
            if (!winner.getRequestFingerprint().equals(fingerprint)) {
                throw new ConflictException("IDEMPOTENCY_CONFLICT", "Idempotency-Key was already used with a different request");
            }
            return new ReservationCreationResult(ReservationResponse.from(winner), false);
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        return ReservationResponse.from(reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(int page, int size, String sort,
                                                  ReservationStatus status, UUID guestId, RoomType roomType,
                                                  LocalDate arrivalFrom, LocalDate arrivalTo,
                                                  LocalDate departureFrom, LocalDate departureTo,
                                                  String assignedRoomNumber) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Sort parsedSort = parseSort(sort);

        Specification<ReservationEntity> spec = Specification.where(null);
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (guestId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("guestId"), guestId));
        if (roomType != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("roomType"), roomType));
        if (arrivalFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("arrivalDate"), arrivalFrom));
        if (arrivalTo != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("arrivalDate"), arrivalTo));
        if (departureFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("departureDate"), departureFrom));
        if (departureTo != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("departureDate"), departureTo));
        if (assignedRoomNumber != null && !assignedRoomNumber.isBlank()) {
            String normalized = assignedRoomNumber.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedRoomNumber"), normalized));
        }

        Page<ReservationEntity> result = reservationRepository.findAll(spec, PageRequest.of(safePage, safeSize, parsedSort));
        List<ReservationResponse> content = result.getContent().stream().map(ReservationResponse::from).toList();
        return PageResponse.of(content, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public ReservationResponse patch(UUID id, PatchReservationCommand command) {
        return ReservationResponse.from(transactionService.patch(id, command));
    }

    public ReservationResponse assignRoom(UUID id, AssignRoomRequest request) {
        String roomNumber = request.roomNumber().trim();
        PatchReservationCommand command = new PatchReservationCommand(
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.of(roomNumber)
        );
        return patch(id, command);
    }

    public ReservationResponse cancel(UUID id) {
        return ReservationResponse.from(transactionService.cancel(id));
    }

    public ReservationResponse markNoShow(UUID id) {
        return ReservationResponse.from(transactionService.markNoShow(id));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(RoomType roomType, LocalDate arrivalDate, LocalDate departureDate) {
        validateAvailabilityDates(arrivalDate, departureDate);
        return availabilityService.availability(roomType, arrivalDate, departureDate, LocalDate.now(hotelClock), null);
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new HotelValidationException("Idempotency-Key header is required");
        }
        String normalized = rawKey.trim();
        if (!normalized.equals(rawKey) || normalized.length() > 128) {
            throw new HotelValidationException("Idempotency-Key must contain 1 to 128 characters without surrounding whitespace");
        }
        return normalized;
    }

    private String normalizeOptionalRoomNumber(String roomNumber) {
        if (roomNumber == null) return null;
        String normalized = roomNumber.trim();
        if (normalized.isEmpty()) {
            throw new HotelValidationException("assignedRoomNumber must not be blank when provided");
        }
        return normalized;
    }

    private void validateAvailabilityDates(LocalDate arrivalDate, LocalDate departureDate) {
        if (arrivalDate == null || departureDate == null) {
            throw new HotelValidationException("arrivalDate and departureDate are required");
        }
        if (arrivalDate.isBefore(LocalDate.now(hotelClock))) {
            throw new HotelValidationException("arrivalDate must not be before hotelToday");
        }
        if (!departureDate.isAfter(arrivalDate)) {
            throw new HotelValidationException("departureDate must be after arrivalDate");
        }
    }

    private Sort parseSort(String sort) {
        String property = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", -1);
            if (parts.length > 2 || !SORT_FIELDS.contains(parts[0].trim())) {
                throw new HotelValidationException("Unsupported reservation sort: " + sort);
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

    public record ReservationCreationResult(ReservationResponse response, boolean created) { }
}
