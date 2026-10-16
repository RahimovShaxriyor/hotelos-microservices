package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DisableStaffUserRequest", description = "Optional payload specifying reason for disabling staff user")
public record DisableStaffUserRequest(
        @Schema(description = "Governance reason for account disabling", example = "Seasonal contract ended")
        String reason
) {}
