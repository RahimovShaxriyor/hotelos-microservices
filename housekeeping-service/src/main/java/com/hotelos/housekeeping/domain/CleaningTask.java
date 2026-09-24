package com.hotelos.housekeeping.domain;

import java.time.Instant;

public class CleaningTask {
    private final String roomNumber;
    private final String correlationId;
    private CleaningStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String assignedCleaner;

    public CleaningTask(String roomNumber) {
        this(roomNumber, null);
    }

    public CleaningTask(String roomNumber, String correlationId) {
        this.roomNumber = roomNumber;
        this.correlationId = correlationId;
        this.status = CleaningStatus.WAITING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public CleaningTask(String roomNumber, String correlationId, CleaningStatus status,
                        Instant createdAt, Instant updatedAt, String assignedCleaner) {
        this.roomNumber = roomNumber;
        this.correlationId = correlationId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.assignedCleaner = assignedCleaner;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public CleaningStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getAssignedCleaner() {
        return assignedCleaner;
    }

    public void assignCleaner(String cleanerName) {
        this.assignedCleaner = cleanerName;
        this.updatedAt = Instant.now();
    }

    public void start() {
        this.status = CleaningStatus.CLEANING;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = CleaningStatus.CLEAN;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = CleaningStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
