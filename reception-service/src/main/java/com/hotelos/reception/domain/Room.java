package com.hotelos.reception.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Room {
    private final String roomNumber;
    private final int floor;
    private final RoomType type;
    private final String proximity;
    private final BigDecimal nightlyRate;
    private RoomStatus status;
    private Instant cleanSince;

    public Room(String roomNumber, int floor, RoomType type, String proximity, BigDecimal nightlyRate,
                RoomStatus status, Instant cleanSince) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.type = type;
        this.proximity = proximity;
        this.nightlyRate = nightlyRate;
        this.status = status;
        this.cleanSince = cleanSince;
    }

    public String getRoomNumber() { return roomNumber; }
    public int getFloor() { return floor; }
    public RoomType getType() { return type; }
    public String getProximity() { return proximity; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public RoomStatus getStatus() { return status; }
    public Instant getCleanSince() { return cleanSince; }

    public void markOccupied() { this.status = RoomStatus.OCCUPIED; }
    public void markDirty() { this.status = RoomStatus.DIRTY; }

    public void updateStatus(RoomStatus newStatus, Instant changedAt) {
        this.status = newStatus;
        if (newStatus == RoomStatus.CLEAN) {
            this.cleanSince = changedAt;
        }
    }
}
