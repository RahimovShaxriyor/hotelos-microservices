package com.hotelos.housekeeping.outbox;

import com.hotelos.housekeeping.persistence.entity.HousekeepingOutboxEventEntity;
import com.hotelos.housekeeping.persistence.repository.HousekeepingOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class HousekeepingOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingOutboxDispatcher.class);

    private final HousekeepingOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final long confirmTimeoutMs;

    public HousekeepingOutboxDispatcher(
            HousekeepingOutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            @Value("${hotelos.outbox.batch-size:10}") int batchSize,
            @Value("${hotelos.outbox.confirm-timeout-ms:3000}") long confirmTimeoutMs
    ) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${hotelos.outbox.poll-interval-ms:500}")
    public void pollAndDispatch() {
        for (int i = 0; i < batchSize; i++) {
            Boolean dispatched = transactionTemplate.execute(status -> dispatchOne());
            if (!Boolean.TRUE.equals(dispatched)) {
                break;
            }
        }
    }

    public boolean dispatchOne() {
        Optional<HousekeepingOutboxEventEntity> candidate = outboxRepository.findNextEligibleEventForUpdate();
        if (candidate.isEmpty()) {
            return false;
        }

        HousekeepingOutboxEventEntity event = candidate.get();

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding("UTF-8");
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setHeader("__TypeId__", "com.hotelos.common.event.EventEnvelope");

        Message message = new Message(event.getMessageBody().getBytes(StandardCharsets.UTF_8), props);
        CorrelationData correlationData = new CorrelationData(event.getEventId().toString());

        try {
            rabbitTemplate.send(event.getExchangeName(), event.getRoutingKey(), message, correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

            if (confirm != null && confirm.isAck() && correlationData.getReturned() == null) {
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                outboxRepository.save(event);
                log.info("Dispatched outbox event {} for aggregate {} (type={})",
                        event.getEventId(), event.getAggregateId(), event.getEventType());
                return true;
            }

            String reason;
            if (correlationData.getReturned() != null) {
                reason = "Message unroutable for routingKey: " + event.getRoutingKey();
            } else if (confirm != null && !confirm.isAck()) {
                reason = "Broker NACK: " + confirm.getReason();
            } else {
                reason = "Broker confirmation missing";
            }
            handleFailure(event, reason);
            return false;
        } catch (TimeoutException ex) {
            handleFailure(event, "Publisher confirm timed out after " + confirmTimeoutMs + "ms");
            return false;
        } catch (Exception ex) {
            handleFailure(event, "Dispatch error: " + ex.getMessage());
            return false;
        }
    }

    private void handleFailure(HousekeepingOutboxEventEntity event, String reason) {
        int nextAttempt = event.getAttemptCount() + 1;
        event.setAttemptCount(nextAttempt);
        event.setNextAttemptAt(Instant.now().plus(calculateBackoff(nextAttempt)));
        event.setLastError(reason);
        outboxRepository.save(event);
        log.warn("Failed to dispatch outbox event {} (attempt {}): {}", event.getEventId(), nextAttempt, reason);
    }

    private Duration calculateBackoff(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofSeconds(1);
            case 2 -> Duration.ofSeconds(2);
            case 3 -> Duration.ofSeconds(5);
            case 4 -> Duration.ofSeconds(10);
            case 5 -> Duration.ofSeconds(30);
            default -> Duration.ofSeconds(60);
        };
    }
}
