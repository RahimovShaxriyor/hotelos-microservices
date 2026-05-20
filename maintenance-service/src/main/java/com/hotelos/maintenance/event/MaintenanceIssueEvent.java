package com.hotelos.maintenance.event;

import java.time.Instant;

public record MaintenanceIssueEvent(String issueId, String roomNumber, String priority, String status,
                                    String assignedTechnician, Instant changedAt) { }
