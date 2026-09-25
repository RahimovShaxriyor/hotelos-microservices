package com.hotelos.reception.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.reception.persistence.entity.ReceptionOutboxEventEntity;
import com.hotelos.reception.persistence.repository.ReceptionOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReceptionOutboxService {
    private static final Logger log = LoggerFactory.getLogger(ReceptionOutboxService.class);

    private final ReceptionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ReceptionOutboxService(ReceptionOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void enqueue(EventEnvelope<T> envelope, String exchange, String routingKey) {
        try {
            String messageBody = objectMapper.writeValueAsString(envelope);
            ReceptionOutboxEventEntity entity = new ReceptionOutboxEventEntity(
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
            log.info("Enqueued outbox event {} for aggregate {} (type={})",
                    envelope.eventId(), envelope.aggregateId(), envelope.eventType());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event " + envelope.eventId(), ex);
        }
    }
}
