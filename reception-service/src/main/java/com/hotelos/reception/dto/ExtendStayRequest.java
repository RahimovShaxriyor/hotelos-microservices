package com.hotelos.reception.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExtendStayRequest(
        @NotNull(message = "newDepartureDate is required")
        LocalDate newDepartureDate
) {
}
