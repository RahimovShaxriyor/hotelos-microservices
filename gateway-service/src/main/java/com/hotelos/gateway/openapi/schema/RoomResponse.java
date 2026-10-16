package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "Room", description = "Hotel room state and operational availability")
public record RoomResponse(
        @Schema(description = "Room number", example = "101")
        String roomNumber,
        @Schema(description = "Floor level", example = "1")
        int floor,
        @Schema(description = "Room category", example = "SINGLE", allowableValues = {"SINGLE", "DOUBLE", "SUITE", "DELUXE"})
        String type,
        @Schema(description = "Architectural proximity preference", example = "LIFT", allowableValues = {"LIFT", "STAIRS", "QUIET", "VIEW"})
        String proximity,
        @Schema(description = "Standard nightly rate in specified currency", example = "150000.00")
        BigDecimal nightlyRate,
        @Schema(description = "ISO-4217 currency code", example = "UZS")
        String currency,
        @Schema(description = "Physical occupancy status", example = "VACANT", allowableValues = {"VACANT", "OCCUPIED"})
        String occupancyStatus,
        @Schema(description = "Housekeeping hygiene status", example = "CLEAN", allowableValues = {"CLEAN", "DIRTY", "IN_PROGRESS"})
        String housekeepingStatus,
        @Schema(description = "Engineering maintenance status", example = "OPERATIONAL", allowableValues = {"OPERATIONAL", "OUT_OF_ORDER", "MAINTENANCE_REQUIRED"})
        String engineeringStatus,
        @Schema(description = "Timestamp when room was certified clean", example = "2026-10-10T08:00:00Z")
        Instant cleanSince,
        @Schema(description = "Whether turnover cleaning cycle is active", example = "false")
        boolean turnoverPending,
        @Schema(description = "Turnover tracking correlation ID", example = "null")
        String turnoverCorrelationId,
        @Schema(description = "Timestamp of last occupancy state change", example = "2026-10-10T08:00:00Z")
        Instant occupancyChangedAt,
        @Schema(description = "Timestamp of last turnover state change", example = "2026-10-10T08:00:00Z")
        Instant turnoverChangedAt,
        @Schema(description = "Whether room is immediately ready and available for check-in", example = "true")
        boolean sellable
) {}
