package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "PatchReservationRequest", description = "Fields to modify on a confirmed reservation")
public record PatchReservationRequest(
        @Schema(description = "Updated arrival date", example = "2026-10-16")
        LocalDate arrivalDate,
        @Schema(description = "Updated departure date", example = "2026-10-19")
        LocalDate departureDate,
        @Schema(description = "Updated room type category", example = "SUITE")
        String roomType,
        @Schema(description = "Updated nightly rate snapshot", example = "200000.00")
        BigDecimal nightlyRateSnapshot
) {}
