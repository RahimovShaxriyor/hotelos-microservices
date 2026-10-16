package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "UpdateStaffRolesRequest", description = "Payload to replace roles for a staff user")
public record UpdateStaffRolesRequest(
        @Schema(description = "Replacement set of role boundaries", example = "[\"MANAGER\", \"RECEPTIONIST\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        Set<String> roles
) {}
