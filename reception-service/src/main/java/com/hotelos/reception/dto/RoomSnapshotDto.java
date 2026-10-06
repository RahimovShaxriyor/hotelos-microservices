package com.hotelos.reception.dto;

import java.time.Instant;

public record RoomSnapshotDto(
        String roomNumber,
        String occupancyStatus,
        Instant occupancyChangedAt,
        boolean turnoverPending,
        String turnoverCorrelationId,
        Instant turnoverChangedAt
) {}
