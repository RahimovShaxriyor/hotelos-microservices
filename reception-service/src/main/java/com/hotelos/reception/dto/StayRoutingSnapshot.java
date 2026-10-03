package com.hotelos.reception.dto;

import com.hotelos.reception.domain.RoomType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StayRoutingSnapshot(
        UUID stayId,
        String currentRoomNumber,
        RoomType currentRoomType,
        LocalDate checkInDate,
        int bookedNights,
        LocalDate departureDate,
        Instant checkedOutAt
) {
}
