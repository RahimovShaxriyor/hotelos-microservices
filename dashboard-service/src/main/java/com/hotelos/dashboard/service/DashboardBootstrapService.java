package com.hotelos.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.dashboard.dto.BootstrapResultDto;
import com.hotelos.dashboard.persistence.repository.RoomProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class DashboardBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DashboardBootstrapService.class);

    private final RoomProjectionRepository roomProjectionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hotelos.services.reception-url:http://reception-service:8081}")
    private String receptionUrl;

    @Value("${hotelos.services.housekeeping-url:http://housekeeping-service:8082}")
    private String housekeepingUrl;

    @Value("${hotelos.services.maintenance-url:http://maintenance-service:8084}")
    private String maintenanceUrl;

    public DashboardBootstrapService(
            RoomProjectionRepository roomProjectionRepository,
            ObjectMapper objectMapper
    ) {
        this.roomProjectionRepository = roomProjectionRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public BootstrapResultDto bootstrapAll(String authHeader) {
        Map<String, Object> sourcesReport = new LinkedHashMap<>();
        int successCount = 0;
        int failureCount = 0;

        // 1. Reception Snapshot
        try {
            int processed = fetchAndApplyReception(authHeader);
            sourcesReport.put("reception", Map.of("status", "SUCCESS", "roomsProcessed", processed));
            successCount++;
        } catch (Exception ex) {
            log.warn("Bootstrap: Reception source failed: {}", ex.getMessage());
            sourcesReport.put("reception", Map.of("status", "FAILED", "error", ex.getMessage()));
            failureCount++;
        }

        // 2. Housekeeping Snapshot
        try {
            int processed = fetchAndApplyHousekeeping(authHeader);
            sourcesReport.put("housekeeping", Map.of("status", "SUCCESS", "roomsProcessed", processed));
            successCount++;
        } catch (Exception ex) {
            log.warn("Bootstrap: Housekeeping source failed: {}", ex.getMessage());
            sourcesReport.put("housekeeping", Map.of("status", "FAILED", "error", ex.getMessage()));
            failureCount++;
        }

        // 3. Maintenance Snapshot
        try {
            int processed = fetchAndApplyMaintenance(authHeader);
            sourcesReport.put("maintenance", Map.of("status", "SUCCESS", "roomsProcessed", processed));
            successCount++;
        } catch (Exception ex) {
            log.warn("Bootstrap: Maintenance source failed: {}", ex.getMessage());
            sourcesReport.put("maintenance", Map.of("status", "FAILED", "error", ex.getMessage()));
            failureCount++;
        }

        String overallStatus = (failureCount == 0) ? "COMPLETED" : (successCount > 0 ? "PARTIALLY_COMPLETED" : "FAILED");
        long totalRooms = roomProjectionRepository.count();

        return new BootstrapResultDto(overallStatus, sourcesReport, (int) totalRooms);
    }

    private int fetchAndApplyReception(String authHeader) throws Exception {
        String url = receptionUrl + "/api/reception/internal/rooms/snapshot";
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        List<Map<String, Object>> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return applyReceptionSnapshot(list);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyReceptionSnapshot(List<Map<String, Object>> snapshot) {
        int count = 0;
        for (Map<String, Object> item : snapshot) {
            String roomNumber = (String) item.get("roomNumber");
            String occupancyStatus = (String) item.get("occupancyStatus");
            Instant occupancyChangedAt = item.get("occupancyChangedAt") != null ? Instant.parse((String) item.get("occupancyChangedAt")) : null;
            Boolean turnoverPending = (Boolean) item.get("turnoverPending");
            String turnoverCorrelationId = (String) item.get("turnoverCorrelationId");
            Instant turnoverChangedAt = item.get("turnoverChangedAt") != null ? Instant.parse((String) item.get("turnoverChangedAt")) : null;

            if (roomNumber != null) {
                roomProjectionRepository.insertRoomIfAbsent(roomNumber);
                if (occupancyStatus != null && occupancyChangedAt != null) {
                    roomProjectionRepository.updateOccupancyIfNewer(roomNumber, occupancyStatus, occupancyChangedAt);
                }
                if (turnoverPending != null && turnoverChangedAt != null) {
                    roomProjectionRepository.updateTurnoverBootstrapIfNewer(
                            roomNumber, turnoverPending, turnoverCorrelationId, turnoverChangedAt
                    );
                }
                count++;
            }
        }
        return count;
    }

    private int fetchAndApplyHousekeeping(String authHeader) throws Exception {
        String url = housekeepingUrl + "/api/housekeeping/internal/rooms/snapshot";
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        List<Map<String, Object>> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return applyHousekeepingSnapshot(list);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyHousekeepingSnapshot(List<Map<String, Object>> snapshot) {
        int count = 0;
        for (Map<String, Object> item : snapshot) {
            String roomNumber = (String) item.get("roomNumber");
            String housekeepingStatus = (String) item.get("housekeepingStatus");
            Instant housekeepingChangedAt = item.get("housekeepingChangedAt") != null ? Instant.parse((String) item.get("housekeepingChangedAt")) : null;

            if (roomNumber != null) {
                roomProjectionRepository.insertRoomIfAbsent(roomNumber);
                if (housekeepingStatus != null && housekeepingChangedAt != null) {
                    roomProjectionRepository.updateHousekeepingIfNewer(roomNumber, housekeepingStatus, housekeepingChangedAt);
                }
                count++;
            }
        }
        return count;
    }

    private int fetchAndApplyMaintenance(String authHeader) throws Exception {
        String url = maintenanceUrl + "/api/maintenance/internal/rooms/snapshot";
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
        }
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        List<Map<String, Object>> list = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        return applyMaintenanceSnapshot(list);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyMaintenanceSnapshot(List<Map<String, Object>> snapshot) {
        int count = 0;
        for (Map<String, Object> item : snapshot) {
            String roomNumber = (String) item.get("roomNumber");
            String engineeringStatus = (String) item.get("engineeringStatus");
            Instant engineeringChangedAt = item.get("engineeringChangedAt") != null ? Instant.parse((String) item.get("engineeringChangedAt")) : null;

            if (roomNumber != null) {
                roomProjectionRepository.insertRoomIfAbsent(roomNumber);
                if (engineeringStatus != null && engineeringChangedAt != null) {
                    roomProjectionRepository.updateEngineeringIfNewer(roomNumber, engineeringStatus, engineeringChangedAt);
                }
                count++;
            }
        }
        return count;
    }
}
