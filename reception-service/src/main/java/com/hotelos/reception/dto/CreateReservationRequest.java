package com.hotelos.reception.dto;

import com.hotelos.reception.domain.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID guestId,
        @NotNull RoomType roomType,
        @NotNull LocalDate arrivalDate,
        @NotNull LocalDate departureDate,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal nightlyRateSnapshot,
        @NotBlank @Size(max = 3) String currency,
        @Size(max = 16) String assignedRoomNumber
) { }
