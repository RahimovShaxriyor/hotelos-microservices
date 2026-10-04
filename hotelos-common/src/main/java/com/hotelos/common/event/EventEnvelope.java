package com.hotelos.common.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String source,
        String aggregateId,
        String correlationId,
        T payload
) {
    public static final int CURRENT_VERSION = 1;

    public static <T> EventEnvelope<T> create(
            String eventType,
            String source,
            String aggregateId,
            String correlationId,
            T payload
    ) {
        return create(eventType, source, aggregateId, correlationId, payload, CURRENT_VERSION);
    }

    public static <T> EventEnvelope<T> create(
            String eventType,
            String source,
            String aggregateId,
            String correlationId,
            T payload,
            int eventVersion
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                eventVersion,
                Instant.now(),
                source,
                aggregateId,
                correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString(),
                payload
        );
    }
}
