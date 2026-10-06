package com.hotelos.housekeeping.dto;

import java.time.Instant;

public record HousekeepingRoomSnapshotDto(
        String roomNumber,
        String housekeepingStatus,
        Instant housekeepingChangedAt
) {}
