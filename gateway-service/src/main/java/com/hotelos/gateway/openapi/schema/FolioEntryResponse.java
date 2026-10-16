package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "FolioEntryResponse", description = "Individual financial line-item recorded on a folio")
public record FolioEntryResponse(
        @Schema(description = "Entry unique ID", example = "00000000-0000-0000-0000-000000000041")
        UUID id,
        @Schema(description = "Parent folio ID", example = "00000000-0000-0000-0000-000000000040")
        UUID folioId,
        @Schema(description = "Currency", example = "UZS")
        String currency,
        @Schema(description = "Entry classification", example = "CHARGE", allowableValues = {"CHARGE", "PAYMENT", "ADJUSTMENT"})
        String entryType,
        @Schema(description = "Originating operational service", example = "RECEPTION", allowableValues = {"RECEPTION", "ROOM_SERVICE", "SYSTEM", "MANUAL"})
        String sourceType,
        @Schema(description = "Reference ID from the originating system", example = "stay-room-night-1")
        String sourceId,
        @Schema(description = "Transaction monetary amount", example = "150000.00")
        BigDecimal amount,
        @Schema(description = "Line-item description", example = "Night 1 Room Charge")
        String description,
        @Schema(description = "User or system actor who created entry", example = "SYSTEM")
        String createdBy,
        @Schema(description = "Timestamp when event occurred", example = "2026-10-10T14:30:00Z")
        Instant occurredAt,
        @Schema(description = "Database creation timestamp", example = "2026-10-10T14:30:00Z")
        Instant createdAt
) {}
