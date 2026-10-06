package com.hotelos.maintenance.dto;

import java.time.Instant;

public record MaintenanceRoomSnapshotDto(
        String roomNumber,
        String engineeringStatus,
        Instant engineeringChangedAt
) {}
