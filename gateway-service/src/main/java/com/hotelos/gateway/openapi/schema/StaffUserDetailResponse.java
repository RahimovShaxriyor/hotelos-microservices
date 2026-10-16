package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "StaffUserDetailResponse", description = "Detailed projection of a staff user account with security metadata")
public record StaffUserDetailResponse(
        @Schema(description = "Staff user unique ID", example = "00000000-0000-0000-0000-000000000001")
        UUID id,
        @Schema(description = "Staff username (email address)", example = "manager@hotelos.internal")
        String username,
        @Schema(description = "Full name of staff member", example = "Alice Smith")
        String fullName,
        @Schema(description = "Set of assigned role boundaries", example = "[\"MANAGER\"]")
        Set<String> roles,
        @Schema(description = "Account operational status", example = "ACTIVE", allowableValues = {"ACTIVE", "DISABLED"})
        String status,
        @Schema(description = "Account classification", example = "STAFF", allowableValues = {"STAFF", "OPERATOR"})
        String accountType,
        @Schema(description = "Failed login attempts counter", example = "0")
        int failedLoginAttempts,
        @Schema(description = "Account lockout expiration timestamp if locked", example = "null")
        Instant lockedUntil,
        @Schema(description = "Account creation timestamp", example = "2026-10-10T10:00:00Z")
        Instant createdAt,
        @Schema(description = "Last account update timestamp", example = "2026-10-10T10:00:00Z")
        Instant updatedAt
) {}
