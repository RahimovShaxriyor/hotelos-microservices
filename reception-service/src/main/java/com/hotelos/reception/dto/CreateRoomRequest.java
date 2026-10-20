package com.hotelos.reception.dto;

import com.hotelos.reception.domain.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRoomRequest(
        @NotBlank @Size(max = 16) String roomNumber,
        @NotNull @Min(1) Integer floor,
        @NotNull RoomType roomType,
        @NotBlank @Size(max = 32) String proximity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal nightlyRate,
        @Size(min = 3, max = 3) String currency
) {}
