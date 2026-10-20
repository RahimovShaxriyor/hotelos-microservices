package com.hotelos.reception.dto;

import com.hotelos.reception.domain.EngineeringStatus;
import com.hotelos.reception.domain.HousekeepingStatus;
import com.hotelos.reception.domain.OccupancyStatus;
import com.hotelos.reception.domain.RoomType;
import com.hotelos.reception.persistence.entity.RoomEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomAdminResponse(
        String roomNumber,
        int floor,
        RoomType roomType,
        String proximity,
        BigDecimal nightlyRate,
        String currency,
        boolean active,
        OccupancyStatus occupancyStatus,
        HousekeepingStatus housekeepingStatus,
        EngineeringStatus engineeringStatus,
        boolean turnoverPending,
        boolean isSellable,
        Instant cleanSince,
        Instant occupancyChangedAt,
        Instant turnoverChangedAt,
        Instant createdAt
) {
    public static RoomAdminResponse from(RoomEntity entity) {
        return new RoomAdminResponse(
                entity.getRoomNumber(),
                entity.getFloor(),
                entity.getType(),
                entity.getProximity(),
                entity.getNightlyRate(),
                entity.getCurrency(),
                entity.isActive(),
                entity.getOccupancyStatus(),
                entity.getHousekeepingStatus(),
                entity.getEngineeringStatus(),
                entity.isTurnoverPending(),
                entity.isSellable(),
                entity.getCleanSince(),
                entity.getOccupancyChangedAt(),
                entity.getTurnoverChangedAt(),
                entity.getCreatedAt()
        );
    }
}
