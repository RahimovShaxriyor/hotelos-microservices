package com.hotelos.housekeeping.service;

import com.hotelos.housekeeping.domain.CleaningStatus;
import com.hotelos.housekeeping.domain.HousekeepingStatus;
import com.hotelos.housekeeping.domain.TaskSource;
import com.hotelos.housekeeping.event.LocalHousekeepingStatusChangedEvent;
import com.hotelos.housekeeping.persistence.entity.CleaningTaskEntity;
import com.hotelos.housekeeping.persistence.entity.RoomStateEntity;
import com.hotelos.housekeeping.persistence.repository.CleaningTaskRepository;
import com.hotelos.housekeeping.persistence.repository.RoomStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TurnoverProcessor {
    private static final Logger log = LoggerFactory.getLogger(TurnoverProcessor.class);

    private final RoomStateRepository roomStateRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TurnoverProcessor(RoomStateRepository roomStateRepository,
                             CleaningTaskRepository cleaningTaskRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.roomStateRepository = roomStateRepository;
        this.cleaningTaskRepository = cleaningTaskRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public boolean executeVacatedMutation(String trimmedRoom, String trimmedCorrelation, List<String> cleaners) {
        roomStateRepository.insertIfAbsent(trimmedRoom);
        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new IllegalStateException("Room state not found immediately after insert: " + trimmedRoom));

        Optional<CleaningTaskEntity> existingCorrelationOpt = cleaningTaskRepository.findByTurnoverCorrelationId(trimmedCorrelation);
        if (existingCorrelationOpt.isPresent()) {
            CleaningTaskEntity existing = existingCorrelationOpt.get();
            if (existing.getRoomNumber().equals(trimmedRoom)) {
                log.info("Turnover {} for room {} has already been processed or is active, ignoring duplicate room.vacated",
                        trimmedCorrelation, trimmedRoom);
                return false;
            } else {
                log.error("DATA INCONSISTENCY: Turnover correlation {} already claimed by room {}, rejecting for room {}",
                        trimmedCorrelation, existing.getRoomNumber(), trimmedRoom);
                return false;
            }
        }

        Optional<CleaningTaskEntity> activeTaskOpt = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom);
        if (activeTaskOpt.isPresent()) {
            CleaningTaskEntity activeTask = activeTaskOpt.get();
            if (activeTask.getTaskSource() == TaskSource.TURNOVER) {
                log.warn("Active TURNOVER cleaning task already exists for room {} with correlation {}, ignoring conflicting turnover request {}",
                        trimmedRoom, activeTask.getTurnoverCorrelationId(), trimmedCorrelation);
                return false;
            } else if (activeTask.getTaskSource() == TaskSource.MANUAL) {
                log.info("Active MANUAL task exists for room {} in state {}, promoting to TURNOVER with correlation {}",
                        trimmedRoom, activeTask.getTaskStatus(), trimmedCorrelation);
                activeTask.promoteToTurnover(trimmedCorrelation);
                cleaningTaskRepository.save(activeTask);
                room.setActiveTurnoverCorrelationId(trimmedCorrelation);
                roomStateRepository.save(room);

                String statusStr = activeTask.getTaskStatus() == CleaningStatus.WAITING
                        ? HousekeepingStatus.DIRTY.name()
                        : HousekeepingStatus.CLEANING.name();
                eventPublisher.publishEvent(new LocalHousekeepingStatusChangedEvent(
                        trimmedRoom,
                        statusStr,
                        room.getStatusChangedAt(),
                        trimmedCorrelation
                ));
                return true;
            }
        }

        String cleaner = assignCleaner(trimmedRoom, cleaners);
        Instant now = Instant.now();
        CleaningTaskEntity newTask = new CleaningTaskEntity(
                UUID.randomUUID(),
                trimmedRoom,
                TaskSource.TURNOVER,
                trimmedCorrelation,
                cleaner,
                now
        );

        // May throw DataIntegrityViolationException on concurrent duplicate or cross-room conflict
        cleaningTaskRepository.saveAndFlush(newTask);

        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        room.setActiveTurnoverCorrelationId(trimmedCorrelation);
        room.setStatusChangedAt(now);
        room.setCleanSince(null);
        roomStateRepository.save(room);

        eventPublisher.publishEvent(new LocalHousekeepingStatusChangedEvent(
                trimmedRoom,
                HousekeepingStatus.DIRTY.name(),
                now,
                trimmedCorrelation
        ));
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<CleaningTaskEntity> findTaskByCorrelation(String correlationId) {
        return cleaningTaskRepository.findByTurnoverCorrelationId(correlationId);
    }

    private String assignCleaner(String roomNumber, List<String> cleaners) {
        if (cleaners != null && !cleaners.isEmpty()) {
            int index = Math.floorMod(roomNumber.hashCode(), cleaners.size());
            return cleaners.get(index);
        }
        return "Unassigned";
    }
}
