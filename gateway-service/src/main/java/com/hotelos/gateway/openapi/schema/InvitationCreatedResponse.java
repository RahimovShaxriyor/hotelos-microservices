package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "InvitationCreatedResponse", description = "Response payload after creating or resending a staff invitation")
public record InvitationCreatedResponse(
        @Schema(description = "Invitation unique identifier", example = "00000000-0000-0000-0000-000000000002")
        UUID id,
        @Schema(description = "Recipient email address", example = "newstaff@hotelos.internal")
        String email,
        @Schema(description = "Recipient full name", example = "Bob Jones")
        String fullName,
        @Schema(description = "Roles to be assigned upon acceptance", example = "[\"HOUSEKEEPING\"]")
        Set<String> roles,
        @Schema(description = "Invitation expiration timestamp", example = "2026-10-15T12:00:00Z")
        Instant expiresAt,
        @Schema(description = "Single-exposure invitation acceptance token", example = "inv-tok-placeholder-4uR")
        String token
) {}
