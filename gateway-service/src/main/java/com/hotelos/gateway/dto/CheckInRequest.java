package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Guest check-in request sent through the API Gateway")
public record CheckInRequest(
        @Schema(description = "ID of existing guest profile (mutually exclusive with guestName)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID guestId,
        @Schema(description = "Full name of guest (mutually exclusive with guestId)", example = "Alice Smith") String guestName,
        @Schema(example = "DOUBLE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "ACCESSIBLE"}) String roomType,
        @Schema(example = "2") Integer nights,
        @Schema(example = "3") Integer preferredFloor,
        @Schema(example = "LIFT", allowableValues = {"LIFT", "STAIRS", "NONE"}) String proximityPreference
) {
    public CheckInRequest(String guestName, String roomType, Integer nights, Integer preferredFloor, String proximityPreference) {
        this(null, guestName, roomType, nights, preferredFloor, proximityPreference);
    }
}
