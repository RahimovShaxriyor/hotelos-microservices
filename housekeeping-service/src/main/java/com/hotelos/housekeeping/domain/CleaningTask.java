package com.hotelos.housekeeping.domain;

import java.time.Instant;

public class CleaningTask {
    private final String roomNumber;
    private CleaningStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String assignedCleaner;

    public CleaningTask(String roomNumber) {
        this.roomNumber = roomNumber;
        this.status = CleaningStatus.WAITING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getRoomNumber() { return roomNumber; }
    public CleaningStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAssignedCleaner() { return assignedCleaner; }

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
