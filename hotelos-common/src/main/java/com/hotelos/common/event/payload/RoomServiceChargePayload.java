package com.hotelos.common.event.payload;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceChargePayload(
        String orderId,
        String roomNumber,
        BigDecimal amount,
        String currency,
        Instant chargedAt
) {
    public RoomServiceChargePayload(String orderId, String roomNumber, BigDecimal amount, Instant chargedAt) {
        this(orderId, roomNumber, amount, "UZS", chargedAt);
    }
}
