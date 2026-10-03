package com.hotelos.reception.dto;

import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.domain.FolioSourceType;
import com.hotelos.reception.persistence.entity.FolioEntryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FolioEntryResponse(
        UUID id,
        UUID folioId,
        String currency,
        FolioEntryType entryType,
        FolioSourceType sourceType,
        String sourceId,
        BigDecimal amount,
        String description,
        String createdBy,
        Instant occurredAt,
        Instant createdAt
) {
    public static FolioEntryResponse from(FolioEntryEntity entity) {
        return new FolioEntryResponse(
                entity.getId(),
                entity.getFolioId(),
                entity.getCurrency(),
                entity.getEntryType(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getAmount(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getOccurredAt(),
                entity.getCreatedAt()
        );
    }
}
