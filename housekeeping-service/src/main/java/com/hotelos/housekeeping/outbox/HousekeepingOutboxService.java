package com.hotelos.housekeeping.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.housekeeping.persistence.entity.HousekeepingOutboxEventEntity;
import com.hotelos.housekeeping.persistence.repository.HousekeepingOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class HousekeepingOutboxService {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingOutboxService.class);

    private final HousekeepingOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public HousekeepingOutboxService(HousekeepingOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void enqueue(EventEnvelope<T> envelope, String exchange, String routingKey) {
        try {
            String messageBody = objectMapper.writeValueAsString(envelope);
            HousekeepingOutboxEventEntity entity = new HousekeepingOutboxEventEntity(
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.eventVersion(),
                    envelope.source(),
                    envelope.aggregateId(),
                    envelope.correlationId(),
                    exchange,
                    routingKey,
                    envelope.occurredAt(),
                    messageBody,
                    null,
                    Instant.now()
            );
            outboxRepository.save(entity);
            log.info("Enqueued housekeeping outbox event {} for aggregate {} (type={})",
                    envelope.eventId(), envelope.aggregateId(), envelope.eventType());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event " + envelope.eventId(), ex);
        }
    }
}
