package com.hotelos.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record EventFeedDto(
        UUID eventId,
        String eventType,
        int eventVersion,
        String source,
        String routingKey,
        String aggregateId,
        String correlationId,
        Instant occurredAt,
        Instant receivedAt,
        String summary,
        String payload
) {}
