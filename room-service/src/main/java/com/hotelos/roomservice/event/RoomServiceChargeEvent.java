package com.hotelos.roomservice.event;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomServiceChargeEvent(String orderId, String roomNumber, BigDecimal amount, Instant chargedAt) { }
