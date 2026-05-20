package com.hotelos.housekeeping.event;

import java.time.Instant;

public record RoomVacatedEvent(String roomNumber, Instant checkedOutAt) { }
