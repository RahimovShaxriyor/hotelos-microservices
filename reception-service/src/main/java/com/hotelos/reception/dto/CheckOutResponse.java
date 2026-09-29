package com.hotelos.reception.dto;

import java.math.BigDecimal;

public record CheckOutResponse(
        String roomNumber,
        String guestName,
        BigDecimal total,
        String currency,
        String message
) {
    public CheckOutResponse(String roomNumber, String guestName, BigDecimal total, String message) {
        this(roomNumber, guestName, total, "UZS", message);
    }
}
