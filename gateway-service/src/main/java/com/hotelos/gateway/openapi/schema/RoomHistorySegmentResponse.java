package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "RoomHistorySegment", description = "Historical room occupancy segment for a stay")
public record RoomHistorySegmentResponse(
        @Schema(description = "Segment unique ID", example = "00000000-0000-0000-0000-000000000050")
        UUID id,
        @Schema(description = "Room number occupied during segment", example = "101")
        String roomNumber,
        @Schema(description = "Segment start timestamp", example = "2026-10-10T14:00:00Z")
        Instant startedAt,
        @Schema(description = "Segment end timestamp", example = "2026-10-11T10:00:00Z")
        Instant endedAt,
        @Schema(description = "Reason for room change", example = "Air conditioning defect")
        String reason
) {}
