package com.hotelos.maintenance.domain;

import java.time.Instant;
import java.util.UUID;

public class MaintenanceIssue {
    private final String issueId;
    private final String roomNumber;
    private final String description;
    private final IssuePriority priority;
    private IssueStatus status;
    private String assignedTechnician;
    private final Instant createdAt;
    private Instant updatedAt;

    public MaintenanceIssue(String roomNumber, String description, IssuePriority priority) {
        this.issueId = UUID.randomUUID().toString();
        this.roomNumber = roomNumber;
        this.description = description;
        this.priority = priority;
        this.status = IssueStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getIssueId() { return issueId; }
    public String getRoomNumber() { return roomNumber; }
    public String getDescription() { return description; }
    public IssuePriority getPriority() { return priority; }
    public IssueStatus getStatus() { return status; }
    public String getAssignedTechnician() { return assignedTechnician; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void assign(String technicianName) {
        this.assignedTechnician = technicianName;
        this.status = IssueStatus.ASSIGNED;
        this.updatedAt = Instant.now();
    }

    public void resolve() {
        this.status = IssueStatus.RESOLVED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = IssueStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
