package com.hotelos.dashboard.controller;

import com.hotelos.dashboard.service.EventForwarder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final EventForwarder eventForwarder;

    public DashboardController(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "dashboard-service");
        result.put("time", Instant.now().toString());
        return result;
    }

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot() {
        return eventForwarder.snapshot();
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events() {
        return eventForwarder.getEvents();
    }

    @DeleteMapping("/events")
    public Map<String, Object> clearEvents() {
        return eventForwarder.clearEvents();
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return eventForwarder.clearEvents();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return eventForwarder.clearEvents();
    }
}
