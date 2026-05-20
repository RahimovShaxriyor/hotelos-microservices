package com.hotelos.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Maintenance issue request sent through the API Gateway")
public record CreateIssueRequest(
        @Schema(example = "115") String roomNumber,
        @Schema(example = "Broken shower") String description,
        @Schema(example = "CRITICAL", allowableValues = {"CRITICAL", "HIGH", "NORMAL", "LOW"}) String priority
) {
}
