package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "EventFeedDto", description = "Auditable domain event published over RabbitMQ message bus")
public record EventFeedResponse(
        @Schema(description = "Event unique ID", example = "00000000-0000-0000-0000-000000000080")
        UUID eventId,
        @Schema(description = "Domain event type name", example = "stay.checked_in")
        String eventType,
        @Schema(description = "Event schema version", example = "1")
        int eventVersion,
        @Schema(description = "Originating microservice", example = "reception-service")
        String source,
        @Schema(description = "AMQP routing key", example = "hotel.reception.stay.checked_in")
        String routingKey,
        @Schema(description = "Target aggregate root ID", example = "00000000-0000-0000-0000-000000000010")
        String aggregateId,
        @Schema(description = "End-to-end request correlation ID", example = "7cdcb4df-c453-4fc7-9735-f4fd73f81122")
        String correlationId,
        @Schema(description = "Event creation timestamp", example = "2026-10-10T14:30:00Z")
        Instant occurredAt,
        @Schema(description = "Dashboard consumer ingestion timestamp", example = "2026-10-10T14:30:01Z")
        Instant receivedAt,
        @Schema(description = "Human-readable summary narrative", example = "Guest Alice Smith checked into room 204")
        String summary,
        @Schema(description = "Raw JSON event payload string", example = "{\"stayId\":\"00000000-0000-0000-0000-000000000010\"}")
        String payload
) {}
