package com.hotelos.common.event.payload;

import java.time.Instant;

public record RoomVacatedPayload(
        String roomNumber,
        Instant vacatedAt
) {}
