package com.hotelos.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.dashboard.config.RabbitConfig;
import com.hotelos.dashboard.websocket.DashboardSocketHandler;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventForwarder {
    private static final int MAX_EVENTS = 200;
    private final DashboardSocketHandler dashboardSocketHandler;
    private final ObjectMapper objectMapper;
    private final List<Map<String, Object>> eventHistory = new CopyOnWriteArrayList<>();

    public EventForwarder(DashboardSocketHandler dashboardSocketHandler, ObjectMapper objectMapper) {
        this.dashboardSocketHandler = dashboardSocketHandler;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.DASHBOARD_QUEUE)
    public void forward(Message message) throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("routingKey", message.getMessageProperties().getReceivedRoutingKey());
        envelope.put("receivedAt", Instant.now().toString());
        envelope.put("payload", new String(message.getBody(), StandardCharsets.UTF_8));
        eventHistory.add(envelope);
        trimHistory();
        dashboardSocketHandler.broadcast(objectMapper.writeValueAsString(envelope));
    }

    public List<Map<String, Object>> getEvents() {
        return new ArrayList<>(eventHistory);
    }

    public Map<String, Object> clearEvents() {
        int removed = eventHistory.size();
        eventHistory.clear();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Dashboard event history cleared");
        result.put("removed", removed);
        return result;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "dashboard-service");
        result.put("events", getEvents());
        result.put("eventCount", eventHistory.size());
        result.put("message", "This service keeps live event history. Full operational snapshot is aggregated by the gateway.");
        return result;
    }

    private void trimHistory() {
        while (eventHistory.size() > MAX_EVENTS) {
            eventHistory.remove(0);
        }
    }
}
