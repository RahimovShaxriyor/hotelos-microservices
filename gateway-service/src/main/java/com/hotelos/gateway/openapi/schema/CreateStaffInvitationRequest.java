package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "CreateStaffInvitationRequest", description = "Payload to invite a new staff member")
public record CreateStaffInvitationRequest(
        @Schema(description = "Staff recipient email address", example = "newstaff@hotelos.internal", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @Schema(description = "Staff recipient full name", example = "Bob Jones", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,
        @Schema(description = "Initial roles to assign upon acceptance", example = "[\"HOUSEKEEPING\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        Set<String> roles
) {}
