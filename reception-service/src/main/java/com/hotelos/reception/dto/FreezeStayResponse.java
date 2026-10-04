package com.hotelos.reception.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FreezeStayResponse(
        UUID stayId,
        String status,
        UUID freezeId,
        Instant frozenAt,
        List<RoomServiceChargeItem> charges,
        BigDecimal totalChargesAmount
) {
}
