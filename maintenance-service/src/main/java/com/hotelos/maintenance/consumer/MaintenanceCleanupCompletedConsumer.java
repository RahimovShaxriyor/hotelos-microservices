package com.hotelos.maintenance.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.payload.MaintenanceCleanupCompletedPayload;
import com.hotelos.maintenance.config.RabbitConfig;
import com.hotelos.maintenance.service.MaintenanceTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MaintenanceCleanupCompletedConsumer {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceCleanupCompletedConsumer.class);

    private final MaintenanceTransactionService txService;
    private final ObjectMapper objectMapper;

    public MaintenanceCleanupCompletedConsumer(MaintenanceTransactionService txService,
                                               ObjectMapper objectMapper) {
        this.txService = txService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.MAINTENANCE_CLEANUP_COMPLETED_QUEUE)
    public void onCleanupCompleted(EventEnvelope<?> envelope) {
        if (envelope == null) {
            log.warn("Received null envelope in MaintenanceCleanupCompletedConsumer; rejecting without requeue");
            return;
        }

        if (!EventTypes.MAINTENANCE_CLEANUP_COMPLETED.equals(envelope.eventType()) || envelope.eventVersion() != 1) {
            log.warn("Invalid eventType [{}] or unsupported version [{}] in MaintenanceCleanupCompletedConsumer; rejecting without requeue",
                    envelope.eventType(), envelope.eventVersion());
            return;
        }

        MaintenanceCleanupCompletedPayload payload;
        try {
            if (envelope.payload() instanceof MaintenanceCleanupCompletedPayload typed) {
                payload = typed;
            } else if (envelope.payload() instanceof Map<?, ?> map) {
                payload = objectMapper.convertValue(map, MaintenanceCleanupCompletedPayload.class);
            } else {
                payload = objectMapper.convertValue(envelope.payload(), MaintenanceCleanupCompletedPayload.class);
            }
        } catch (Exception ex) {
            log.error("Malformed payload in MaintenanceCleanupCompletedConsumer: {}; rejecting without requeue", ex.getMessage());
            return;
        }

        if (payload == null || payload.issueId() == null || payload.roomNumber() == null || payload.roomNumber().isBlank() || payload.cleanedAt() == null) {
            log.error("Incomplete payload fields in MaintenanceCleanupCompletedConsumer; rejecting without requeue");
            return;
        }

        txService.processCleanupCompletedWithInbox(envelope, payload);
    }
}
