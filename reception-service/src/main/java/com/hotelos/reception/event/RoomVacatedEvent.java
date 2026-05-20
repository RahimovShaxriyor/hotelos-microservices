package com.hotelos.reception.event;

import java.time.Instant;

public record RoomVacatedEvent(String roomNumber, Instant checkedOutAt) { }
