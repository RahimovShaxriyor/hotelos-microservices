package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "RoomAdminPageResponse", description = "Paginated list of rooms for administration")
public record RoomAdminPageResponse(
        @Schema(description = "List of rooms on current page")
        List<RoomAdminResponse> content,
        @Schema(description = "Current page number (0-indexed)", example = "0")
        int page,
        @Schema(description = "Page size limit", example = "20")
        int size,
        @Schema(description = "Total number of matching rooms", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
) {}
