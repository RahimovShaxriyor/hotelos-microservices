package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Guest check-in request sent through the API Gateway")
public record CheckInRequest(
        @Schema(example = "Diana Otayeva") String guestName,
        @Schema(example = "DOUBLE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "ACCESSIBLE"}) String roomType,
        @Schema(example = "2") Integer nights,
        @Schema(example = "3") Integer preferredFloor,
        @Schema(example = "LIFT", allowableValues = {"LIFT", "STAIRS", "NONE"}) String proximityPreference
) {
}
