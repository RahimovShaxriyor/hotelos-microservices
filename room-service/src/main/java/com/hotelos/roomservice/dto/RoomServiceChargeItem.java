package com.hotelos.roomservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceChargeItem(
        String orderId,
        String roomNumber,
        BigDecimal amount,
        String currency,
        Instant chargedAt
) {
}
