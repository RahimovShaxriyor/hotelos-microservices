package com.hotelos.reception.dto;

import java.math.BigDecimal;

public record CheckOutResponse(String roomNumber, String guestName, BigDecimal total, String message) { }
