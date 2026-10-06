package com.hotelos.dashboard.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class EventFeedEntity {

    private UUID eventId;
    private String eventType;
    private int eventVersion;
    private String source;
    private String routingKey;
    private String aggregateId;
    private String correlationId;
    private Instant occurredAt;
    private Instant receivedAt;
    private String summary;
    private String sanitizedPayload;

    public EventFeedEntity() {}

    public EventFeedEntity(UUID eventId, String eventType, int eventVersion, String source,
                           String routingKey, String aggregateId, String correlationId,
                           Instant occurredAt, Instant receivedAt, String summary, String sanitizedPayload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.source = source;
        this.routingKey = routingKey;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.summary = summary;
        this.sanitizedPayload = sanitizedPayload;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getEventVersion() { return eventVersion; }
    public String getSource() { return source; }
    public String getRoutingKey() { return routingKey; }
    public String getAggregateId() { return aggregateId; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getSummary() { return summary; }
    public String getSanitizedPayload() { return sanitizedPayload; }

    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventVersion(int eventVersion) { this.eventVersion = eventVersion; }
    public void setSource(String source) { this.source = source; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setSanitizedPayload(String sanitizedPayload) { this.sanitizedPayload = sanitizedPayload; }
}
