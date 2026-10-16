package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CreateReservationRequest", description = "Payload to create a new room reservation")
public record CreateReservationRequest(
        @Schema(description = "Guest profile ID", example = "00000000-0000-0000-0000-000000000020", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID guestId,
        @Schema(description = "Room type category", example = "DOUBLE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "DELUXE"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String roomType,
        @Schema(description = "Planned arrival date", example = "2026-10-15", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate arrivalDate,
        @Schema(description = "Planned departure date", example = "2026-10-18", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate departureDate,
        @Schema(description = "Agreed nightly rate snapshot", example = "150000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal nightlyRateSnapshot,
        @Schema(description = "Currency code", example = "UZS", requiredMode = Schema.RequiredMode.REQUIRED)
        String currency,
        @Schema(description = "Optional pre-assigned physical room number", example = "204")
        String assignedRoomNumber
) {}
