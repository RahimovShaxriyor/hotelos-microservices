package com.hotelos.common.event.payload;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceChargePayload(
        String orderId,
        String roomNumber,
        BigDecimal amount,
        Instant chargedAt
) {}
