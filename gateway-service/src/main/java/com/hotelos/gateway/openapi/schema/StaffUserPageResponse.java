package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "StaffUserPageResponse", description = "Paginated list of staff user accounts")
public record StaffUserPageResponse(
        @Schema(description = "Page items")
        List<StaffUserSummaryResponse> items,
        @Schema(description = "Total number of matching elements", example = "42")
        long total,
        @Schema(description = "Current page index (0-based)", example = "0")
        int page,
        @Schema(description = "Page size", example = "20")
        int size,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages
) {}
