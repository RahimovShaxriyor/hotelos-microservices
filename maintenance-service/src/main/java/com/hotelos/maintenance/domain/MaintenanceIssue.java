package com.hotelos.maintenance.domain;

import com.hotelos.maintenance.persistence.entity.MaintenanceIssueEntity;

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
    private final IssueSource source;
    private final String sourceReferenceId;
    private final boolean requiresCleaning;
    private final EngineeringImpact engineeringImpact;
    private final boolean cleanupPending;
    private final Instant cleanupAcknowledgedAt;

    public MaintenanceIssue(String roomNumber, String description, IssuePriority priority) {
        this(UUID.randomUUID().toString(), roomNumber, description, priority, IssueStatus.OPEN, null,
                Instant.now(), Instant.now(), IssueSource.DIRECT, null, false, EngineeringImpact.OUT_OF_ORDER, false, null);
    }

    public MaintenanceIssue(String issueId, String roomNumber, String description, IssuePriority priority,
                            IssueStatus status, String assignedTechnician, Instant createdAt, Instant updatedAt) {
        this(issueId, roomNumber, description, priority, status, assignedTechnician, createdAt, updatedAt,
                IssueSource.DIRECT, null, false, EngineeringImpact.OUT_OF_ORDER, false, null);
    }

    public MaintenanceIssue(String issueId, String roomNumber, String description, IssuePriority priority,
                            IssueStatus status, String assignedTechnician, Instant createdAt, Instant updatedAt,
                            IssueSource source, String sourceReferenceId, boolean requiresCleaning,
                            EngineeringImpact engineeringImpact, boolean cleanupPending, Instant cleanupAcknowledgedAt) {
        this.issueId = issueId;
        this.roomNumber = roomNumber;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedTechnician = assignedTechnician;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.source = source != null ? source : IssueSource.DIRECT;
        this.sourceReferenceId = sourceReferenceId;
        this.requiresCleaning = requiresCleaning;
        this.engineeringImpact = engineeringImpact != null ? engineeringImpact : EngineeringImpact.OUT_OF_ORDER;
        this.cleanupPending = cleanupPending;
        this.cleanupAcknowledgedAt = cleanupAcknowledgedAt;
    }

    public static MaintenanceIssue fromEntity(MaintenanceIssueEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MaintenanceIssue(
                entity.getId().toString(),
                entity.getRoomNumber(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getAssignedTechnician(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSource(),
                entity.getSourceReferenceId(),
                entity.isRequiresCleaning(),
                entity.getEngineeringImpact(),
                entity.isCleanupPending(),
                entity.getCleanupAcknowledgedAt()
        );
    }

    public String getIssueId() {
        return issueId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getDescription() {
        return description;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public IssueSource getSource() {
        return source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public boolean isRequiresCleaning() {
        return requiresCleaning;
    }

    public EngineeringImpact getEngineeringImpact() {
        return engineeringImpact;
    }

    public boolean isCleanupPending() {
        return cleanupPending;
    }

    public Instant getCleanupAcknowledgedAt() {
        return cleanupAcknowledgedAt;
    }

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
