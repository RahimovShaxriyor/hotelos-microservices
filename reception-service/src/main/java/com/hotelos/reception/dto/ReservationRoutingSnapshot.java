package com.hotelos.reception.dto;

import com.hotelos.reception.domain.ReservationStatus;
import com.hotelos.reception.domain.RoomType;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationRoutingSnapshot(
        UUID reservationId,
        RoomType roomType,
        String assignedRoomNumber,
        ReservationStatus status,
        LocalDate arrivalDate,
        LocalDate departureDate
) {
}
