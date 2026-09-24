package com.hotelos.housekeeping.event;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomHousekeepingStatusChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HousekeepingEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public HousekeepingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(LocalHousekeepingStatusChangedEvent event) {
        try {
            RoomHousekeepingStatusChangedPayload payload = new RoomHousekeepingStatusChangedPayload(
                    event.roomNumber(),
                    event.housekeepingStatus(),
                    event.changedAt()
            );
            EventEnvelope<RoomHousekeepingStatusChangedPayload> envelope = EventEnvelope.create(
                    EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                    "housekeeping-service",
                    event.roomNumber(),
                    event.turnoverCorrelationId(),
                    payload
            );
            rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_HOUSEKEEPING_CHANGED, envelope);
            log.info("Published housekeeping status changed event: room={}, status={}, correlationId={}",
                    event.roomNumber(), event.housekeepingStatus(), event.turnoverCorrelationId());
        } catch (Exception ex) {
            log.error("Failed to publish housekeeping room status changed event for room {}: {}",
                    event.roomNumber(), ex.getMessage(), ex);
        }
    }
}
