package com.hotelos.common.event.payload;

import java.time.Instant;

public record MaintenanceIssueUpdatedPayload(
        String issueId,
        String roomNumber,
        String priority,
        String status,
        String assignedTechnician,
        Instant changedAt
) {}
