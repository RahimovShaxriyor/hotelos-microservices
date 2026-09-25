package com.hotelos.roomservice.event;

import org.springframework.stereotype.Component;

/**
 * Superseded by Transactional Outbox (RoomServiceOutboxService + RoomServiceOutboxDispatcher) in P1.5.
 * Direct AFTER_COMMIT RabbitMQ publishing has been removed to prevent duplicate publishing and eliminate dual-write gap.
 */
@Component
public class RoomServiceEventPublisher {
}
