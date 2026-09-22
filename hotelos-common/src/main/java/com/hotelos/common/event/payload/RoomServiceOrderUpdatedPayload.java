package com.hotelos.common.event.payload;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceOrderUpdatedPayload(
        String orderId,
        String roomNumber,
        String status,
        BigDecimal total,
        Instant changedAt
) {}
