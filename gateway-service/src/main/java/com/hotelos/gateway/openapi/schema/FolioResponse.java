package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "FolioResponse", description = "Guest accounting folio linked to stay")
public record FolioResponse(
        @Schema(description = "Folio ID", example = "00000000-0000-0000-0000-000000000040")
        UUID id,
        @Schema(description = "Associated stay ID", example = "00000000-0000-0000-0000-000000000010")
        UUID stayId,
        @Schema(description = "Accounting currency", example = "UZS")
        String currency,
        @Schema(description = "Folio status", example = "OPEN", allowableValues = {"OPEN", "CLOSED"})
        String status,
        @Schema(description = "Current outstanding balance", example = "325000.00")
        BigDecimal balance,
        @Schema(description = "Closing timestamp if closed", example = "null")
        Instant closedAt,
        @Schema(description = "Creation timestamp", example = "2026-10-10T14:30:00Z")
        Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-10-10T14:30:00Z")
        Instant updatedAt
) {}
