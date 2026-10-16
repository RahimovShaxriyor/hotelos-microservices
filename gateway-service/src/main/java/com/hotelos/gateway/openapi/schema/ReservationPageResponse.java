package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ReservationPageResponse", description = "Paginated list of room reservations")
public record ReservationPageResponse(
        @Schema(description = "Page items")
        List<ReservationResponse> items,
        @Schema(description = "Total count of matching reservations", example = "15")
        long totalElements,
        @Schema(description = "Current page index (0-based)", example = "0")
        int page,
        @Schema(description = "Page size", example = "20")
        int size,
        @Schema(description = "Total pages", example = "1")
        int totalPages
) {}
