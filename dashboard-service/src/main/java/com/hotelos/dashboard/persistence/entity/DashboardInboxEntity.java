package com.hotelos.dashboard.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class DashboardInboxEntity {

    private UUID eventId;
    private String eventType;
    private String source;
    private String aggregateId;
    private String correlationId;
    private String envelopeHash;
    private Instant receivedAt;
    private Instant processedAt;

    public DashboardInboxEntity() {}

    public DashboardInboxEntity(UUID eventId, String eventType, String source, String aggregateId,
                                String correlationId, String envelopeHash, Instant receivedAt, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.source = source;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.envelopeHash = envelopeHash;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getSource() { return source; }
    public String getAggregateId() { return aggregateId; }
    public String getCorrelationId() { return correlationId; }
    public String getEnvelopeHash() { return envelopeHash; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
}
