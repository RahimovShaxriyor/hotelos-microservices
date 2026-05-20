package com.hotelos.housekeeping.event;

import java.time.Instant;

public record RoomStatusChangedEvent(String roomNumber, String status, Instant changedAt) { }
