package com.hotelos.roomservice.event;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomServiceChargePayload;
import com.hotelos.common.event.payload.RoomServiceOrderUpdatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoomServiceEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(RoomServiceEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RoomServiceEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderUpdated(LocalOrderUpdatedEvent event) {
        try {
            RoomServiceOrderUpdatedPayload payload = new RoomServiceOrderUpdatedPayload(
                    event.orderId(),
                    event.roomNumber(),
                    event.status(),
                    event.total(),
                    event.changedAt()
            );
            EventEnvelope<RoomServiceOrderUpdatedPayload> envelope = EventEnvelope.create(
                    EventTypes.ROOM_SERVICE_ORDER_UPDATED,
                    "room-service",
                    event.orderId(),
                    event.correlationId(),
                    payload
            );
            rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_SERVICE_ORDER_UPDATED, envelope);
            log.info("Published order updated event for order {} with status {} (correlationId={})",
                    event.orderId(), event.status(), event.correlationId());
        } catch (Exception ex) {
            log.error("Failed to publish order updated event after commit for order {} (correlationId={}): {}",
                    event.orderId(), event.correlationId(), ex.getMessage(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDelivered(LocalOrderDeliveredEvent event) {
        try {
            RoomServiceChargePayload payload = new RoomServiceChargePayload(
                    event.orderId(),
                    event.roomNumber(),
                    event.amount(),
                    event.chargedAt()
            );
            EventEnvelope<RoomServiceChargePayload> envelope = EventEnvelope.create(
                    EventTypes.ROOM_SERVICE_CHARGE,
                    "room-service",
                    event.orderId(),
                    event.correlationId(),
                    payload
            );
            rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_SERVICE_CHARGE, envelope);
            log.info("Published room service charge event for order {} amount {} (correlationId={})",
                    event.orderId(), event.amount(), event.correlationId());
        } catch (Exception ex) {
            log.error("Failed to publish room service charge event after commit for order {} (correlationId={}): {}",
                    event.orderId(), event.correlationId(), ex.getMessage(), ex);
        }
    }
}
