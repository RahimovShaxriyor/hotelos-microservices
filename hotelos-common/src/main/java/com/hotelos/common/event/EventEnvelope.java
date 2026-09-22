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
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                CURRENT_VERSION,
                Instant.now(),
                source,
                aggregateId,
                correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString(),
                payload
        );
    }
}
