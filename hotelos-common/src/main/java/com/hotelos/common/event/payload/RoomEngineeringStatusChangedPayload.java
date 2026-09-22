package com.hotelos.common.event.payload;

import java.time.Instant;

public record RoomEngineeringStatusChangedPayload(
        String roomNumber,
        String engineeringStatus,
        Instant changedAt
) {
}
