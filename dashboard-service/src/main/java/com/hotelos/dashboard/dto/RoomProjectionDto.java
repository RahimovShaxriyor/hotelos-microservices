package com.hotelos.dashboard.dto;

import java.time.Instant;

public record RoomProjectionDto(
        String roomNumber,
        String occupancyStatus,
        String housekeepingStatus,
        String engineeringStatus,
        Boolean turnoverPending,
        boolean sellable,
        Instant occupancyChangedAt,
        Instant housekeepingChangedAt,
        Instant engineeringChangedAt,
        Instant turnoverChangedAt,
        Instant updatedAt
) {
    public static RoomProjectionDto from(
            String roomNumber,
            String occupancyStatus,
            String housekeepingStatus,
            String engineeringStatus,
            Boolean turnoverPending,
            Instant occupancyChangedAt,
            Instant housekeepingChangedAt,
            Instant engineeringChangedAt,
            Instant turnoverChangedAt,
            Instant updatedAt
    ) {
        boolean sellable = "VACANT".equals(occupancyStatus)
                && "CLEAN".equals(housekeepingStatus)
                && "OPERATIONAL".equals(engineeringStatus)
                && Boolean.FALSE.equals(turnoverPending);
        return new RoomProjectionDto(
                roomNumber,
                occupancyStatus,
                housekeepingStatus,
                engineeringStatus,
                turnoverPending,
                sellable,
                occupancyChangedAt,
                housekeepingChangedAt,
                engineeringChangedAt,
                turnoverChangedAt,
                updatedAt
        );
    }
}
