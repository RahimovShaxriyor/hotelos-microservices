package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckInResponse", description = "Confirmation result after executing guest check-in")
public record CheckInResponse(
        @Schema(description = "Created stay unique ID", example = "00000000-0000-0000-0000-000000000010")
        String stayId,
        @Schema(description = "Guest profile ID", example = "00000000-0000-0000-0000-000000000020")
        String guestId,
        @Schema(description = "Guest full name", example = "Alice Smith")
        String guestName,
        @Schema(description = "Assigned room number", example = "204")
        String roomNumber,
        @Schema(description = "Room type", example = "DOUBLE")
        String roomType,
        @Schema(description = "New room occupancy status", example = "OCCUPIED")
        String occupancyStatus,
        @Schema(description = "Currency code", example = "UZS")
        String currency,
        @Schema(description = "Human-readable confirmation message", example = "Check-in successful")
        String message
) {}
