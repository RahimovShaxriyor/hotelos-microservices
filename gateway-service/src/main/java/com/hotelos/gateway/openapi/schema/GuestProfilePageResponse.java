package com.hotelos.gateway.openapi.schema;

import com.hotelos.common.dto.guest.GuestProfileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged guest profiles response")
public record GuestProfilePageResponse(
        @Schema(description = "List of guest profiles on current page")
        List<GuestProfileResponse> content,
        @Schema(description = "Current page number (0-indexed)", example = "0")
        int page,
        @Schema(description = "Page size limit", example = "20")
        int size,
        @Schema(description = "Total number of matching guest profiles", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
) {}
