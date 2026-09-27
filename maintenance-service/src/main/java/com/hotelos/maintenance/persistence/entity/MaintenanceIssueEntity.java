package com.hotelos.maintenance.persistence.entity;

import com.hotelos.maintenance.domain.IssuePriority;
import com.hotelos.maintenance.domain.IssueStatus;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_issues", schema = "maintenance")
public class MaintenanceIssueEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 16, nullable = false)
    private IssuePriority priority;

    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private IssueStatus status;

    @Column(name = "assigned_technician", length = 64)
    private String assignedTechnician;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    protected MaintenanceIssueEntity() {
    }

    public MaintenanceIssueEntity(UUID id, String roomNumber, String description, IssuePriority priority, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.roomNumber = roomNumber;
        this.description = description;
        this.priority = priority;
        this.priorityRank = priority.getRank();
        this.status = IssueStatus.OPEN;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = this.createdAt;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
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

    public int getPriorityRank() {
        return priorityRank;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assign(String technicianName, Instant now) {
        this.assignedTechnician = technicianName;
        this.assignedAt = now;
        this.status = IssueStatus.ASSIGNED;
        this.updatedAt = now;
    }

    public void resolve(Instant now) {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = now;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        this.status = IssueStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
    }
}
