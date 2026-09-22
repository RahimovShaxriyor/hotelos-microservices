package com.hotelos.reception.event;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomOccupancyChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.reception.domain.OccupancyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class ReceptionDomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ReceptionDomainEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ReceptionDomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCheckIn(CheckInCommittedEvent event) {
        log.info("Transaction committed: publishing room.occupancy.changed for room {}", event.roomNumber());
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                event.roomNumber(),
                null,
                new RoomOccupancyChangedPayload(event.roomNumber(), event.occupancyStatus().name(), Instant.now())
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED, occupancyEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCheckOut(CheckOutCommittedEvent event) {
        log.info("Transaction committed: publishing checkout events for room {} with correlationId {}",
                event.roomNumber(), event.correlationId());

        // 1. Publish occupancy changed (VACANT)
        EventEnvelope<RoomOccupancyChangedPayload> occupancyEvent = EventEnvelope.create(
                EventTypes.ROOM_OCCUPANCY_CHANGED,
                "reception-service",
                event.roomNumber(),
                event.correlationId(),
                new RoomOccupancyChangedPayload(event.roomNumber(), OccupancyStatus.VACANT.name(), Instant.now())
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_OCCUPANCY_CHANGED, occupancyEvent);

        // 2. Publish room vacated with shared correlationId
        EventEnvelope<RoomVacatedPayload> vacatedEvent = EventEnvelope.create(
                EventTypes.ROOM_VACATED,
                "reception-service",
                event.roomNumber(),
                event.correlationId(),
                new RoomVacatedPayload(event.roomNumber(), Instant.now())
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_VACATED, vacatedEvent);
    }
}
