package com.hotelos.housekeeping.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomHousekeepingStatusChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.housekeeping.domain.CleaningStatus;
import com.hotelos.housekeeping.domain.HousekeepingStatus;
import com.hotelos.housekeeping.domain.TaskSource;
import com.hotelos.housekeeping.inbox.HousekeepingInboxService;
import com.hotelos.housekeeping.outbox.HousekeepingOutboxService;
import com.hotelos.housekeeping.persistence.entity.CleaningTaskEntity;
import com.hotelos.housekeeping.persistence.entity.RoomStateEntity;
import com.hotelos.housekeeping.persistence.repository.CleaningTaskRepository;
import com.hotelos.housekeeping.persistence.repository.RoomStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final HousekeepingOutboxService outboxService;
    private final HousekeepingInboxService inboxService;

    public TurnoverProcessor(RoomStateRepository roomStateRepository,
                             CleaningTaskRepository cleaningTaskRepository,
                             HousekeepingOutboxService outboxService,
                             HousekeepingInboxService inboxService) {
        this.roomStateRepository = roomStateRepository;
        this.cleaningTaskRepository = cleaningTaskRepository;
        this.outboxService = outboxService;
        this.inboxService = inboxService;
    }

    /**
     * Transaction T1: Atomic Inbox registration + Turnover business mutation + Outbox status event insert.
     * If an unhandled exception or data integrity violation occurs, T1 rolls back completely.
     */
    @Transactional
    public boolean processVacatedWithInbox(EventEnvelope<RoomVacatedPayload> envelope, List<String> cleaners) {
        HousekeepingInboxService.InboxResult inboxResult = inboxService.registerEvent(envelope);
        if (inboxResult == HousekeepingInboxService.InboxResult.DUPLICATE || inboxResult == HousekeepingInboxService.InboxResult.CONFLICT) {
            return false;
        }

        RoomVacatedPayload payload = envelope.payload();
        String trimmedRoom = payload.roomNumber().trim();
        String trimmedCorrelation = envelope.correlationId().trim();

        return executeVacatedMutation(trimmedRoom, trimmedCorrelation, cleaners);
    }

    /**
     * Transaction T2: Executed after T1 rollback to classify unique constraint conflicts on turnover_correlation_id.
     * Durably records the event in inbox_events if handled so future redeliveries are immediate no-ops.
     * Returns true if the violation was NOT due to turnoverCorrelationId (caller should rethrow).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean classifyAndResolveConflict(EventEnvelope<RoomVacatedPayload> envelope, String trimmedRoom, String trimmedCorrelation) {
        Optional<CleaningTaskEntity> conflictOpt = cleaningTaskRepository.findByTurnoverCorrelationId(trimmedCorrelation);
        if (conflictOpt.isPresent()) {
            CleaningTaskEntity conflict = conflictOpt.get();
            if (!conflict.getRoomNumber().equals(trimmedRoom)) {
                log.error("DATA INCONSISTENCY: Concurrent turnover correlation {} already claimed by room {}, rejected for room {}",
                        trimmedCorrelation, conflict.getRoomNumber(), trimmedRoom);
            } else {
                log.info("Concurrent duplicate turnover correlation {} ignored for room {}", trimmedCorrelation, trimmedRoom);
            }
            inboxService.recordConsumedEvent(envelope);
            return false; // Handled conflict: acknowledge cleanly
        }
        return true; // Not a turnover correlation conflict; rethrow
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

                outboxService.enqueue(
                        EventEnvelope.create(
                                EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                                "housekeeping-service",
                                trimmedRoom,
                                trimmedCorrelation,
                                new RoomHousekeepingStatusChangedPayload(trimmedRoom, statusStr, room.getStatusChangedAt())
                        ),
                        MessagingConstants.HOTEL_EXCHANGE,
                        RoutingKeys.ROOM_HOUSEKEEPING_CHANGED
                );
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

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                        "housekeeping-service",
                        trimmedRoom,
                        trimmedCorrelation,
                        new RoomHousekeepingStatusChangedPayload(trimmedRoom, HousekeepingStatus.DIRTY.name(), now)
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_HOUSEKEEPING_CHANGED
        );
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
