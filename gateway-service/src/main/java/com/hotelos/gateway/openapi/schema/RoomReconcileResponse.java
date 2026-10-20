package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "RoomReconcileResponse", description = "Result of safe occupancy reconciliation against active stays")
public record RoomReconcileResponse(
        @Schema(description = "Room number", example = "101")
        String roomNumber,
        @Schema(description = "Occupancy status prior to reconciliation", example = "OCCUPIED")
        String previousOccupancyStatus,
        @Schema(description = "Occupancy status after reconciliation", example = "VACANT")
        String currentOccupancyStatus,
        @Schema(description = "Whether occupancy status was changed during reconciliation", example = "true")
        boolean changed,
        @Schema(description = "Reconciliation audit rationale", example = "Active stay 00000000-0000-0000-0000-000000000001 is present; repaired to OCCUPIED")
        String reconciliationReason,
        @Schema(description = "Timestamp when reconciliation was performed", example = "2026-10-16T12:00:00Z")
        Instant reconciledAt
) {}
