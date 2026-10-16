package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcceptStaffInvitationRequest", description = "Payload to accept staff invitation and activate account")
public record AcceptStaffInvitationRequest(
        @Schema(description = "Single-use invitation token", example = "inv-tok-placeholder-4uR", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,
        @Schema(description = "Password chosen by recipient meeting complexity policy", example = "P@ssword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {}
