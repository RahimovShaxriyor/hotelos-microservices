package com.hotelos.housekeeping.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.payload.MaintenanceWorkCompletedPayload;
import com.hotelos.housekeeping.config.RabbitConfig;
import com.hotelos.housekeeping.service.HousekeepingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MaintenanceWorkCompletedConsumer {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceWorkCompletedConsumer.class);

    private final HousekeepingService housekeepingService;
    private final ObjectMapper objectMapper;

    public MaintenanceWorkCompletedConsumer(HousekeepingService housekeepingService,
                                           ObjectMapper objectMapper) {
        this.housekeepingService = housekeepingService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.HOUSEKEEPING_WORK_COMPLETED_QUEUE)
    public void onWorkCompleted(EventEnvelope<?> envelope) {
        if (envelope == null) {
            log.warn("Received null envelope in MaintenanceWorkCompletedConsumer; rejecting without requeue");
            return;
        }

        if (!EventTypes.MAINTENANCE_WORK_COMPLETED.equals(envelope.eventType()) || envelope.eventVersion() != 1) {
            log.warn("Invalid eventType [{}] or unsupported version [{}] in MaintenanceWorkCompletedConsumer; rejecting without requeue",
                    envelope.eventType(), envelope.eventVersion());
            return;
        }

        MaintenanceWorkCompletedPayload payload;
        try {
            if (envelope.payload() instanceof MaintenanceWorkCompletedPayload typed) {
                payload = typed;
            } else if (envelope.payload() instanceof Map<?, ?> map) {
                payload = objectMapper.convertValue(map, MaintenanceWorkCompletedPayload.class);
            } else {
                payload = objectMapper.convertValue(envelope.payload(), MaintenanceWorkCompletedPayload.class);
            }
        } catch (Exception ex) {
            log.error("Malformed payload in MaintenanceWorkCompletedConsumer: {}; rejecting without requeue", ex.getMessage());
            return;
        }

        if (payload == null || payload.issueId() == null || payload.roomNumber() == null || payload.roomNumber().isBlank() || payload.completedAt() == null) {
            log.error("Incomplete payload fields in MaintenanceWorkCompletedConsumer; rejecting without requeue");
            return;
        }

        housekeepingService.processWorkCompletedWithInbox(envelope, payload);
    }
}
