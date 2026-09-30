package com.hotelos.reception.service;

import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.dto.CreateReservationRequest;
import com.hotelos.reception.dto.PatchReservationCommand;
import com.hotelos.reception.exception.ConflictException;
import com.hotelos.reception.exception.HotelValidationException;
import com.hotelos.reception.exception.ResourceNotFoundException;
import com.hotelos.reception.persistence.entity.ReservationEntity;
import com.hotelos.reception.persistence.repository.GuestRepository;
import com.hotelos.reception.persistence.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationTransactionService {
    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final RoomInventoryAvailabilityService availabilityService;
    private final Clock hotelClock;

    public ReservationTransactionService(ReservationRepository reservationRepository,
                                         GuestRepository guestRepository,
                                         RoomInventoryAvailabilityService availabilityService,
                                         Clock hotelClock) {
        this.reservationRepository = reservationRepository;
        this.guestRepository = guestRepository;
        this.availabilityService = availabilityService;
        this.hotelClock = hotelClock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreationOutcome create(CreateReservationRequest request, String idempotencyKey, String fingerprint,
                                  String currency, String assignedRoomNumber) {
        ReservationEntity existing = reservationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) return replay(existing, fingerprint);

        validateDates(request.arrivalDate(), request.departureDate());
        if (!guestRepository.existsById(request.guestId())) {
            throw new ResourceNotFoundException("Guest profile with ID " + request.guestId() + " not found");
        }

        availabilityService.lockRoomTypes(List.of(request.roomType()));
        existing = reservationRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) return replay(existing, fingerprint);

        LocalDate today = LocalDate.now(hotelClock);
        availabilityService.assertCapacity(
                request.roomType(), request.arrivalDate(), request.departureDate(), today, null
        );
        if (assignedRoomNumber != null) {
            availabilityService.lockAndValidateAssignedRoom(
                    assignedRoomNumber, request.roomType(), request.arrivalDate(), request.departureDate(), today, null
            );
        }

        ReservationEntity reservation = new ReservationEntity(
                UUID.randomUUID(), request.guestId(), request.roomType(), assignedRoomNumber,
                request.arrivalDate(), request.departureDate(), request.nightlyRateSnapshot(),
                currency, idempotencyKey, fingerprint
        );
        return new CreationOutcome(reservationRepository.saveAndFlush(reservation), true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationEntity patch(UUID id, PatchReservationCommand command) {
        ReservationEntity reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        requireConfirmed(reservation);

        RoomType requestedType = command.roomType().orElse(reservation.getRoomType());
        availabilityService.lockRoomTypes(List.of(reservation.getRoomType(), requestedType));

        LocalDate arrivalDate = command.arrivalDate().orElse(reservation.getArrivalDate());
        LocalDate departureDate = command.departureDate().orElse(reservation.getDepartureDate());
        RoomType roomType = command.roomType().orElse(reservation.getRoomType());
        String assignedRoom = command.assignedRoomNumber().orElse(reservation.getAssignedRoomNumber());
        validateDates(arrivalDate, departureDate);

        if (roomType != reservation.getRoomType()
                && reservation.getAssignedRoomNumber() != null
                && command.assignedRoomNumber().isEmpty()) {
            throw new ConflictException(
                    "ASSIGNED_ROOM_REQUIRES_REPLACEMENT",
                    "Changing roomType requires a compatible assignedRoomNumber in the same PATCH"
            );
        }

        LocalDate today = LocalDate.now(hotelClock);
        availabilityService.assertCapacity(roomType, arrivalDate, departureDate, today, reservation.getId());
        if (assignedRoom != null) {
            availabilityService.lockAndValidateAssignedRoom(
                    assignedRoom, roomType, arrivalDate, departureDate, today, reservation.getId()
            );
        }

        reservation.setArrivalDate(arrivalDate);
        reservation.setDepartureDate(departureDate);
        reservation.setRoomType(roomType);
        reservation.setAssignedRoomNumber(assignedRoom);
        return reservationRepository.save(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationEntity cancel(UUID id) {
        ReservationEntity reservation = lock(id);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) return reservation;
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("INVALID_RESERVATION_TRANSITION", "Only a confirmed reservation can be cancelled");
        }
        reservation.cancel(Instant.now(hotelClock));
        return reservationRepository.save(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationEntity markNoShow(UUID id) {
        ReservationEntity reservation = lock(id);
        if (reservation.getStatus() == ReservationStatus.NO_SHOW) return reservation;
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("INVALID_RESERVATION_TRANSITION", "Only a confirmed reservation can be marked no-show");
        }
        if (!LocalDate.now(hotelClock).isAfter(reservation.getArrivalDate())) {
            throw new ConflictException("NO_SHOW_TOO_EARLY", "Reservation cannot be marked no-show before the day after arrivalDate");
        }
        reservation.markNoShow(Instant.now(hotelClock));
        return reservationRepository.save(reservation);
    }

    private ReservationEntity lock(UUID id) {
        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private void validateDates(LocalDate arrivalDate, LocalDate departureDate) {
        LocalDate today = LocalDate.now(hotelClock);
        if (arrivalDate.isBefore(today)) {
            throw new HotelValidationException("arrivalDate must not be before hotelToday");
        }
        if (!departureDate.isAfter(arrivalDate)) {
            throw new HotelValidationException("departureDate must be after arrivalDate");
        }
    }

    private void requireConfirmed(ReservationEntity reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("RESERVATION_NOT_MUTABLE", "Only a confirmed reservation can be changed");
        }
    }

    private CreationOutcome replay(ReservationEntity existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new ConflictException("IDEMPOTENCY_CONFLICT", "Idempotency-Key was already used with a different request");
        }
        return new CreationOutcome(existing, false);
    }

    public record CreationOutcome(ReservationEntity reservation, boolean created) { }
}
