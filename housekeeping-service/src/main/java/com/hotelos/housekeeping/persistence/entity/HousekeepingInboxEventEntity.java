package com.hotelos.housekeeping.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_events", schema = "housekeeping")
public class HousekeepingInboxEventEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "envelope_hash", nullable = false, length = 64)
    private String envelopeHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected HousekeepingInboxEventEntity() {}

    public HousekeepingInboxEventEntity(
            UUID eventId,
            String eventType,
            String source,
            String aggregateId,
            String correlationId,
            String envelopeHash,
            Instant receivedAt,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.source = source;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.envelopeHash = envelopeHash;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSource() {
        return source;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getEnvelopeHash() {
        return envelopeHash;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
