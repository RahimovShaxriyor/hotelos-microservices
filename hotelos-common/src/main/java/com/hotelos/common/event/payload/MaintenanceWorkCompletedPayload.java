package com.hotelos.common.event.payload;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceWorkCompletedPayload(
        UUID issueId,
        String roomNumber,
        boolean requiresCleaning,
        Instant completedAt
) {
}
