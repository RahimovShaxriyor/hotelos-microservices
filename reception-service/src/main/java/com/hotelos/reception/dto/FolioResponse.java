package com.hotelos.reception.dto;

import com.hotelos.reception.domain.FolioStatus;
import com.hotelos.reception.persistence.entity.FolioEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FolioResponse(
        UUID id,
        UUID stayId,
        String currency,
        FolioStatus status,
        BigDecimal balance,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static FolioResponse from(FolioEntity entity, BigDecimal balance) {
        return new FolioResponse(
                entity.getId(),
                entity.getStayId(),
                entity.getCurrency(),
                entity.getStatus(),
                balance != null ? balance : BigDecimal.ZERO,
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
