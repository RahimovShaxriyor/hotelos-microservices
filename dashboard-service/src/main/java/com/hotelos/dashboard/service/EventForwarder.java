package com.hotelos.dashboard.service;

import com.hotelos.dashboard.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EventForwarder {

    private static final Logger log = LoggerFactory.getLogger(EventForwarder.class);

    private final DashboardProjectionService dashboardProjectionService;

    public EventForwarder(DashboardProjectionService dashboardProjectionService) {
        this.dashboardProjectionService = dashboardProjectionService;
    }

    @RabbitListener(queues = RabbitConfig.DASHBOARD_QUEUE)
    public void forward(Message message) {
        String routingKey = message.getMessageProperties() != null
                ? message.getMessageProperties().getReceivedRoutingKey()
                : "unknown";
        byte[] body = message.getBody();
        try {
            dashboardProjectionService.processMessage(routingKey, body);
        } catch (Exception ex) {
            log.error("Unhandled error processing message from routingKey {}: {}", routingKey, ex.getMessage(), ex);
            throw ex; // Allow Rabbit retry on unhandled transient DB/system exception
        }
    }
}
