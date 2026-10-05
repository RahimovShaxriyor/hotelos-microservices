package com.hotelos.maintenance.persistence.entity;

import com.hotelos.common.util.CanonicalTime;
import com.hotelos.maintenance.domain.EngineeringImpact;
import com.hotelos.maintenance.domain.IssuePriority;
import com.hotelos.maintenance.domain.IssueSource;
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

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 32, nullable = false)
    private IssueSource source = IssueSource.DIRECT;

    @Column(name = "source_reference_id", length = 64)
    private String sourceReferenceId;

    @Column(name = "requires_cleaning", nullable = false)
    private boolean requiresCleaning = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "engineering_impact", length = 32, nullable = false)
    private EngineeringImpact engineeringImpact = EngineeringImpact.OUT_OF_ORDER;

    @Column(name = "cleanup_pending", nullable = false)
    private boolean cleanupPending = false;

    @Column(name = "cleanup_acknowledged_at")
    private Instant cleanupAcknowledgedAt;

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
        this.source = IssueSource.DIRECT;
        this.engineeringImpact = EngineeringImpact.OUT_OF_ORDER;
        this.requiresCleaning = false;
        this.cleanupPending = false;
        this.createdAt = CanonicalTime.normalize(createdAt != null ? createdAt : Instant.now());
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public IssueSource getSource() {
        return source;
    }

    public void setSource(IssueSource source) {
        this.source = source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(String sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }

    public boolean isRequiresCleaning() {
        return requiresCleaning;
    }

    public void setRequiresCleaning(boolean requiresCleaning) {
        this.requiresCleaning = requiresCleaning;
    }

    public EngineeringImpact getEngineeringImpact() {
        return engineeringImpact;
    }

    public void setEngineeringImpact(EngineeringImpact engineeringImpact) {
        this.engineeringImpact = engineeringImpact;
    }

    public boolean isCleanupPending() {
        return cleanupPending;
    }

    public void setCleanupPending(boolean cleanupPending) {
        this.cleanupPending = cleanupPending;
    }

    public Instant getCleanupAcknowledgedAt() {
        return cleanupAcknowledgedAt;
    }

    public void setCleanupAcknowledgedAt(Instant cleanupAcknowledgedAt) {
        this.cleanupAcknowledgedAt = CanonicalTime.normalize(cleanupAcknowledgedAt);
    }

    public void assign(String technicianName, Instant now) {
        this.assignedTechnician = technicianName;
        this.assignedAt = CanonicalTime.normalize(now);
        this.status = IssueStatus.ASSIGNED;
        this.updatedAt = this.assignedAt;
    }

    public void resolve(Instant now) {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = CanonicalTime.normalize(now);
        this.updatedAt = this.resolvedAt;
    }

    public void cancel(Instant now) {
        this.status = IssueStatus.CANCELLED;
        this.cancelledAt = CanonicalTime.normalize(now);
        this.updatedAt = this.cancelledAt;
    }
}
