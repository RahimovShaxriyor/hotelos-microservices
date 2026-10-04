package com.hotelos.roomservice.dto;

import com.hotelos.roomservice.domain.OrderingSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FreezeStayResponse(
        UUID stayId,
        OrderingSessionStatus status,
        UUID freezeId,
        Instant frozenAt,
        List<RoomServiceChargeItem> charges,
        BigDecimal totalChargesAmount
) {
}
