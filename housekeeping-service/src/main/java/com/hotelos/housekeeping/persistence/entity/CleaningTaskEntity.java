package com.hotelos.housekeeping.persistence.entity;

import com.hotelos.housekeeping.domain.CleaningStatus;
import com.hotelos.housekeeping.domain.TaskSource;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cleaning_tasks", schema = "housekeeping")
public class CleaningTaskEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "room_number", length = 16, nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_source", length = 16, nullable = false)
    private TaskSource taskSource;

    @Column(name = "turnover_correlation_id", length = 64)
    private String turnoverCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", length = 16, nullable = false)
    private CleaningStatus taskStatus;

    @Column(name = "assigned_cleaner", length = 64, nullable = false)
    private String assignedCleaner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Transient
    private boolean isNew = true;

    protected CleaningTaskEntity() {
    }

    public CleaningTaskEntity(UUID id, String roomNumber, TaskSource taskSource, String turnoverCorrelationId,
                              String assignedCleaner, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.roomNumber = roomNumber;
        this.taskSource = taskSource;
        this.turnoverCorrelationId = turnoverCorrelationId;
        this.taskStatus = CleaningStatus.WAITING;
        this.assignedCleaner = assignedCleaner;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
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

    public TaskSource getTaskSource() {
        return taskSource;
    }

    public void setTaskSource(TaskSource taskSource) {
        this.taskSource = taskSource;
    }

    public String getTurnoverCorrelationId() {
        return turnoverCorrelationId;
    }

    public void setTurnoverCorrelationId(String turnoverCorrelationId) {
        this.turnoverCorrelationId = turnoverCorrelationId;
    }

    public CleaningStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(CleaningStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getAssignedCleaner() {
        return assignedCleaner;
    }

    public void setAssignedCleaner(String assignedCleaner) {
        this.assignedCleaner = assignedCleaner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void start(Instant now) {
        this.taskStatus = CleaningStatus.CLEANING;
        this.startedAt = now;
    }

    public void complete(Instant now) {
        this.taskStatus = CleaningStatus.CLEAN;
        this.completedAt = now;
    }

    public void cancel(Instant now) {
        this.taskStatus = CleaningStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void promoteToTurnover(String correlationId) {
        this.taskSource = TaskSource.TURNOVER;
        this.turnoverCorrelationId = correlationId;
    }
}
