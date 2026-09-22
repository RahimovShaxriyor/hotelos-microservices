package com.hotelos.common.event.payload;

import java.time.Instant;

public record RoomOccupancyChangedPayload(
        String roomNumber,
        String occupancyStatus,
        Instant changedAt
) {
}
