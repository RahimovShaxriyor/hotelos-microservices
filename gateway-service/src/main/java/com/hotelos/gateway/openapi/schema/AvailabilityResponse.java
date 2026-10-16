package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "AvailabilityResponse", description = "Room category inventory availability check for a date range")
public record AvailabilityResponse(
        @Schema(description = "Requested room category", example = "DOUBLE")
        String roomType,
        @Schema(description = "Arrival date", example = "2026-10-15")
        LocalDate arrivalDate,
        @Schema(description = "Departure date", example = "2026-10-18")
        LocalDate departureDate,
        @Schema(description = "Minimum available rooms across the date range", example = "2")
        int availableInventory,
        @Schema(description = "Day-by-day availability breakdown")
        List<DailyAvailabilityResponse> daily
) {}
