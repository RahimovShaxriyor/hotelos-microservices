package com.hotelos.maintenance.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.maintenance.persistence.entity.MaintenanceOutboxEventEntity;
import com.hotelos.maintenance.persistence.repository.MaintenanceOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MaintenanceOutboxService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceOutboxService.class);

    private final MaintenanceOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceOutboxService(MaintenanceOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void enqueue(EventEnvelope<T> envelope, String exchange, String routingKey) {
        try {
            String messageBody = objectMapper.writeValueAsString(envelope);
            MaintenanceOutboxEventEntity entity = new MaintenanceOutboxEventEntity(
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
