package com.hotelos.reception.dto;

import com.hotelos.reception.domain.OccupancyStatus;

import java.time.Instant;

public record RoomReconcileResponse(
        String roomNumber,
        OccupancyStatus previousOccupancyStatus,
        OccupancyStatus currentOccupancyStatus,
        boolean changed,
        String reconciliationReason,
        Instant reconciledAt
) {}
