package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "FolioEntryPageResponse", description = "Paginated list of folio entry line items")
public record FolioEntryPageResponse(
        @Schema(description = "Page items")
        List<FolioEntryResponse> content,
        @Schema(description = "Total elements count", example = "5")
        long totalElements,
        @Schema(description = "Current page index (0-based)", example = "0")
        int number,
        @Schema(description = "Page size", example = "20")
        int size,
        @Schema(description = "Total pages", example = "1")
        int totalPages
) {}
