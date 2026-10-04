package com.hotelos.common.event.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RoomServiceChargePayloadV2(
        String orderId,
        UUID stayId,
        String roomNumber,
        BigDecimal amount,
        String currency,
        Instant chargedAt
) {
}
