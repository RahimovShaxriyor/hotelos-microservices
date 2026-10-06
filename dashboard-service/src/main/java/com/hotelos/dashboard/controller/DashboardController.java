package com.hotelos.dashboard.controller;

import com.hotelos.dashboard.dto.BootstrapResultDto;
import com.hotelos.dashboard.dto.EventFeedDto;
import com.hotelos.dashboard.service.DashboardBootstrapService;
import com.hotelos.dashboard.service.DashboardQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardQueryService queryService;
    private final DashboardBootstrapService bootstrapService;

    public DashboardController(
            DashboardQueryService queryService,
            DashboardBootstrapService bootstrapService
    ) {
        this.queryService = queryService;
        this.bootstrapService = bootstrapService;
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
        return queryService.getSnapshot();
    }

    @GetMapping("/events")
    public List<EventFeedDto> events() {
        return queryService.getRecentEvents();
    }

    @DeleteMapping("/events")
    public Map<String, Object> clearEvents() {
        return queryService.clearEvents();
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<BootstrapResultDto> bootstrap(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        BootstrapResultDto result = bootstrapService.bootstrapAll(authHeader);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return queryService.clearEvents();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return queryService.clearEvents();
    }

    @PostMapping("/dev/bootstrap")
    public ResponseEntity<BootstrapResultDto> devBootstrap(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        return bootstrap(authHeader);
    }
}
