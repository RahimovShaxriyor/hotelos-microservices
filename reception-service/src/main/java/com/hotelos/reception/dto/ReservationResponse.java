package com.hotelos.reception.dto;

import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.persistence.entity.ReservationEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID guestId,
        RoomType roomType,
        String assignedRoomNumber,
        LocalDate arrivalDate,
        LocalDate departureDate,
        ReservationStatus status,
        BigDecimal nightlyRateSnapshot,
        String currency,
        Instant cancelledAt,
        Instant noShowAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReservationResponse from(ReservationEntity entity) {
        return new ReservationResponse(
                entity.getId(), entity.getGuestId(), entity.getRoomType(), entity.getAssignedRoomNumber(),
                entity.getArrivalDate(), entity.getDepartureDate(), entity.getStatus(),
                entity.getNightlyRateSnapshot(), entity.getCurrency(), entity.getCancelledAt(),
                entity.getNoShowAt(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
