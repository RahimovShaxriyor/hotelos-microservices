package com.hotelos.common.event.payload;

import java.time.Instant;

public record RoomHousekeepingStatusChangedPayload(
        String roomNumber,
        String housekeepingStatus,
        Instant changedAt
) {
}
