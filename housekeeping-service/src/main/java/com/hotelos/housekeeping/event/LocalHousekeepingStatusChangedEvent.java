package com.hotelos.housekeeping.event;

import java.time.Instant;

public record LocalHousekeepingStatusChangedEvent(
        String roomNumber,
        String housekeepingStatus,
        Instant changedAt,
        String turnoverCorrelationId
) {}
