package com.hotelos.reception.dto;

import com.hotelos.reception.domain.RoomType;

import java.time.LocalDate;
import java.util.Optional;

public record PatchReservationCommand(
        Optional<LocalDate> arrivalDate,
        Optional<LocalDate> departureDate,
        Optional<RoomType> roomType,
        Optional<String> assignedRoomNumber
) { }
