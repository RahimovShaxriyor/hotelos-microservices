package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "StayPageResponse", description = "Paginated list of guest stays")
public record StayPageResponse(
        @Schema(description = "Page items")
        List<StayResponse> items,
        @Schema(description = "Total count of matching stays", example = "8")
        long totalElements,
        @Schema(description = "Current page index (0-based)", example = "0")
        int page,
        @Schema(description = "Page size", example = "20")
        int size,
        @Schema(description = "Total pages", example = "1")
        int totalPages
) {}
