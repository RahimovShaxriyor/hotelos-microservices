package com.hotelos.dashboard.service;

import com.hotelos.dashboard.persistence.repository.EventFeedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCleanupService {

    private static final Logger log = LoggerFactory.getLogger(DashboardCleanupService.class);

    private final EventFeedRepository eventFeedRepository;

    public DashboardCleanupService(EventFeedRepository eventFeedRepository) {
        this.eventFeedRepository = eventFeedRepository;
    }

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @Transactional
    public int triggerCleanup() {
        try {
            int deleted = eventFeedRepository.deleteOldEventsExceedingRetention();
            if (deleted > 0) {
                log.info("Retention cleanup: deleted {} events exceeding max 5000 rows in event_feed", deleted);
            }
            return deleted;
        } catch (Exception ex) {
            log.warn("Failed to run event feed retention cleanup: {}", ex.getMessage());
            return 0;
        }
    }
}
