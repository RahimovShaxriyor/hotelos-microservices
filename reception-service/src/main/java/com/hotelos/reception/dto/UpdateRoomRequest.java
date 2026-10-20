package com.hotelos.reception.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRoomRequest(
        @Min(1) Integer floor,
        @Size(max = 32) String proximity,
        @DecimalMin(value = "0.01") BigDecimal nightlyRate
) {}
