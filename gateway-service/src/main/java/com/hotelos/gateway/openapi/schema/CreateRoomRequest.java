package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "CreateRoomRequest", description = "Payload to create a new physical hotel room")
public record CreateRoomRequest(
        @Schema(description = "Room number identifier", example = "301", requiredMode = Schema.RequiredMode.REQUIRED)
        String roomNumber,
        @Schema(description = "Floor level", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer floor,
        @Schema(description = "Room type category", example = "SUITE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "ACCESSIBLE"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String roomType,
        @Schema(description = "Architectural proximity preference", example = "QUIET", allowableValues = {"LIFT", "STAIRS", "QUIET", "VIEW"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String proximity,
        @Schema(description = "Standard nightly rate in specified currency", example = "250000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal nightlyRate,
        @Schema(description = "ISO-4217 currency code (defaults to hotel currency if omitted)", example = "UZS")
        String currency
) {}
