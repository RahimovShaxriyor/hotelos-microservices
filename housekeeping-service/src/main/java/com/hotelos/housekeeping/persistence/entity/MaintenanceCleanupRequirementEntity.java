package com.hotelos.housekeeping.persistence.entity;

import com.hotelos.common.util.CanonicalTime;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_cleanup_requirements", schema = "housekeeping")
public class MaintenanceCleanupRequirementEntity implements Persistable<UUID> {

    @Id
    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Column(name = "maintenance_completed_at", nullable = false)
    private Instant maintenanceCompletedAt;

    @Column(name = "satisfied_at")
    private Instant satisfiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected MaintenanceCleanupRequirementEntity() {
    }

    public MaintenanceCleanupRequirementEntity(UUID issueId, String roomNumber, Instant maintenanceCompletedAt, Instant createdAt) {
        this.issueId = issueId;
        this.roomNumber = roomNumber;
        this.maintenanceCompletedAt = CanonicalTime.normalize(maintenanceCompletedAt);
        this.createdAt = CanonicalTime.normalize(createdAt != null ? createdAt : Instant.now());
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return issueId;
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

    public UUID getIssueId() {
        return issueId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public Instant getMaintenanceCompletedAt() {
        return maintenanceCompletedAt;
    }

    public Instant getSatisfiedAt() {
        return satisfiedAt;
    }

    public void setSatisfiedAt(Instant satisfiedAt) {
        this.satisfiedAt = CanonicalTime.normalize(satisfiedAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
