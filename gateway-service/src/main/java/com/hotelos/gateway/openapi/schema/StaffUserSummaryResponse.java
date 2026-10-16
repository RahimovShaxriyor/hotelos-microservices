package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "StaffUserSummaryResponse", description = "Summary projection of a staff user account")
public record StaffUserSummaryResponse(
        @Schema(description = "Staff user unique ID", example = "00000000-0000-0000-0000-000000000001")
        UUID id,
        @Schema(description = "Staff username (email address)", example = "receptionist@hotelos.internal")
        String username,
        @Schema(description = "Full name of staff member", example = "Jane Doe")
        String fullName,
        @Schema(description = "Set of assigned role boundaries", example = "[\"RECEPTIONIST\"]")
        Set<String> roles,
        @Schema(description = "Account operational status", example = "ACTIVE", allowableValues = {"ACTIVE", "DISABLED"})
        String status,
        @Schema(description = "Account classification", example = "STAFF", allowableValues = {"STAFF", "OPERATOR"})
        String accountType,
        @Schema(description = "Account creation timestamp", example = "2026-10-10T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Last account update timestamp", example = "2026-10-10T12:00:00Z")
        Instant updatedAt
) {}
