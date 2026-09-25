package com.hotelos.housekeeping.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "housekeeping")
public class HousekeepingOutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "exchange_name", nullable = false, length = 128)
    private String exchangeName;

    @Column(name = "routing_key", nullable = false, length = 128)
    private String routingKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "message_headers", columnDefinition = "TEXT")
    private String messageHeaders;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    protected HousekeepingOutboxEventEntity() {}

    public HousekeepingOutboxEventEntity(
            UUID eventId,
            String eventType,
            int eventVersion,
            String source,
            String aggregateId,
            String correlationId,
            String exchangeName,
            String routingKey,
            Instant occurredAt,
            String messageBody,
            String messageHeaders,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.source = source;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.occurredAt = occurredAt;
        this.messageBody = messageBody;
        this.messageHeaders = messageHeaders;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.attemptCount = 0;
        this.nextAttemptAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
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

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getMessageHeaders() {
        return messageHeaders;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
