package com.hotelos.reception.event;

import org.springframework.stereotype.Component;

/**
 * Superseded by Transactional Outbox (ReceptionOutboxService + ReceptionOutboxDispatcher) in P1.5.
 * Direct AFTER_COMMIT RabbitMQ publishing has been removed to prevent duplicate publishing and eliminate dual-write gap.
 */
@Component
public class ReceptionDomainEventPublisher {
}
