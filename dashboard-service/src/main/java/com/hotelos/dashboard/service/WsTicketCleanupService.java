package com.hotelos.dashboard.service;

import com.hotelos.dashboard.repository.WsTicketConsumptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class WsTicketCleanupService {

    private static final Logger log = LoggerFactory.getLogger(WsTicketCleanupService.class);
    private static final Duration RETENTION_PERIOD = Duration.ofMinutes(5);

    private final WsTicketConsumptionRepository consumptionRepository;

    public WsTicketCleanupService(WsTicketConsumptionRepository consumptionRepository) {
        this.consumptionRepository = consumptionRepository;
    }

    /**
     * Periodically cleans up consumption records that expired more than RETENTION_PERIOD ago.
     * Ensures active and recently expired tickets are never removed prematurely.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void cleanupExpiredConsumptions() {
        try {
            Instant threshold = Instant.now().minus(RETENTION_PERIOD);
            consumptionRepository.deleteExpiredBefore(threshold);
        } catch (Exception ex) {
            log.error("Failed to cleanup expired WebSocket tickets: {}", ex.getMessage());
        }
    }
}
