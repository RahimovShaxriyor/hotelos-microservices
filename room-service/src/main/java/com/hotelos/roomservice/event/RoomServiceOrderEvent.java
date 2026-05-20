package com.hotelos.roomservice.event;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceOrderEvent(String orderId, String roomNumber, String status, BigDecimal total, Instant changedAt) { }
