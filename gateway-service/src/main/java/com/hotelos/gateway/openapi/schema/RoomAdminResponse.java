package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "RoomAdminResponse", description = "Detailed administrative representation of a physical room")
public record RoomAdminResponse(
        @Schema(description = "Room number", example = "301")
        String roomNumber,
        @Schema(description = "Floor level", example = "3")
        int floor,
        @Schema(description = "Room category", example = "SUITE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "ACCESSIBLE"})
        String roomType,
        @Schema(description = "Architectural proximity preference", example = "QUIET")
        String proximity,
        @Schema(description = "Standard nightly rate in specified currency", example = "250000.00")
        BigDecimal nightlyRate,
        @Schema(description = "ISO-4217 currency code", example = "UZS")
        String currency,
        @Schema(description = "Whether the room is active in inventory lifecycle", example = "true")
        boolean active,
        @Schema(description = "Physical occupancy status", example = "VACANT", allowableValues = {"VACANT", "OCCUPIED"})
        String occupancyStatus,
        @Schema(description = "Housekeeping hygiene status", example = "CLEAN", allowableValues = {"CLEAN", "DIRTY", "IN_PROGRESS"})
        String housekeepingStatus,
        @Schema(description = "Engineering maintenance status", example = "OPERATIONAL", allowableValues = {"OPERATIONAL", "OUT_OF_ORDER", "MAINTENANCE_REQUIRED"})
        String engineeringStatus,
        @Schema(description = "Whether turnover cleaning cycle is active", example = "false")
        boolean turnoverPending,
        @Schema(description = "Whether room is sellable according to strict operational invariant", example = "true")
        boolean isSellable,
        @Schema(description = "Timestamp when room was certified clean", example = "2026-10-10T08:00:00Z")
        Instant cleanSince,
        @Schema(description = "Timestamp of last occupancy state change", example = "2026-10-10T08:00:00Z")
        Instant occupancyChangedAt,
        @Schema(description = "Timestamp of last turnover state change", example = "2026-10-10T08:00:00Z")
        Instant turnoverChangedAt,
        @Schema(description = "Timestamp when room was created", example = "2026-10-10T08:00:00Z")
        Instant createdAt
) {}
