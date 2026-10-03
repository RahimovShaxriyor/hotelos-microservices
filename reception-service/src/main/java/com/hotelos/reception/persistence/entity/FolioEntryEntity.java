package com.hotelos.reception.persistence.entity;

import com.hotelos.reception.domain.FolioEntryType;
import com.hotelos.reception.domain.FolioSourceType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "folio_entries", schema = "reception")
public class FolioEntryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "folio_id", nullable = false)
    private UUID folioId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 30, nullable = false)
    private FolioEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30, nullable = false)
    private FolioSourceType sourceType;

    @Column(name = "source_id", length = 100, nullable = false)
    private String sourceId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "idempotency_fingerprint", length = 64)
    private String idempotencyFingerprint;

    @Column(name = "created_by", length = 64, nullable = false)
    private String createdBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FolioEntryEntity() {}

    public FolioEntryEntity(
            UUID id,
            UUID folioId,
            String currency,
            FolioEntryType entryType,
            FolioSourceType sourceType,
            String sourceId,
            BigDecimal amount,
            String description,
            String idempotencyFingerprint,
            String createdBy,
            Instant occurredAt,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.folioId = folioId;
        this.currency = currency;
        this.entryType = entryType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.amount = amount;
        this.description = description;
        this.idempotencyFingerprint = idempotencyFingerprint;
        this.createdBy = createdBy != null ? createdBy : "SYSTEM";
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFolioId() { return folioId; }
    public void setFolioId(UUID folioId) { this.folioId = folioId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public FolioEntryType getEntryType() { return entryType; }
    public void setEntryType(FolioEntryType entryType) { this.entryType = entryType; }

    public FolioSourceType getSourceType() { return sourceType; }
    public void setSourceType(FolioSourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    public void setIdempotencyFingerprint(String idempotencyFingerprint) { this.idempotencyFingerprint = idempotencyFingerprint; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FolioEntryEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
