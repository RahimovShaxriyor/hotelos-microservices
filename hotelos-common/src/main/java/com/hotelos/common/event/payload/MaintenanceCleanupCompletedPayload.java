package com.hotelos.common.event.payload;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceCleanupCompletedPayload(
        UUID issueId,
        String roomNumber,
        Instant cleanedAt
) {
}
