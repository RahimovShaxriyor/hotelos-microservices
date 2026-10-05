package com.hotelos.housekeeping.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.MaintenanceCleanupCompletedPayload;
import com.hotelos.common.event.payload.MaintenanceWorkCompletedPayload;
import com.hotelos.common.event.payload.RoomHousekeepingStatusChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.common.util.CanonicalTime;
import com.hotelos.housekeeping.client.MaintenanceInternalClient;
import com.hotelos.housekeeping.config.RabbitConfig;
import com.hotelos.housekeeping.domain.*;
import com.hotelos.housekeeping.dto.ReportDefectRequest;
import com.hotelos.housekeeping.exception.ConflictException;
import com.hotelos.housekeeping.exception.HotelValidationException;
import com.hotelos.housekeeping.inbox.HousekeepingInboxService;
import com.hotelos.housekeeping.outbox.HousekeepingOutboxService;
import com.hotelos.housekeeping.persistence.entity.CleaningTaskEntity;
import com.hotelos.housekeeping.persistence.entity.MaintenanceCleanupRequirementEntity;
import com.hotelos.housekeeping.persistence.entity.RoomStateEntity;
import com.hotelos.housekeeping.persistence.repository.CleaningTaskRepository;
import com.hotelos.housekeeping.persistence.repository.HousekeepingInboxRepository;
import com.hotelos.housekeeping.persistence.repository.HousekeepingOutboxRepository;
import com.hotelos.housekeeping.persistence.repository.MaintenanceCleanupRequirementRepository;
import com.hotelos.housekeeping.persistence.repository.RoomStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class HousekeepingService {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingService.class);

    private final RoomStateRepository roomStateRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final TurnoverProcessor turnoverProcessor;
    private final HousekeepingOutboxService outboxService;
    private final HousekeepingOutboxRepository outboxRepository;
    private final HousekeepingInboxRepository inboxRepository;
    private final MaintenanceCleanupRequirementRepository requirementRepository;
    private final MaintenanceInternalClient maintenanceInternalClient;
    private final HousekeepingInboxService inboxService;
    private final List<String> cleaners = List.of("Cleaner-1", "Cleaner-2", "Cleaner-3");

    public HousekeepingService(RoomStateRepository roomStateRepository,
                               CleaningTaskRepository cleaningTaskRepository,
                               TurnoverProcessor turnoverProcessor,
                               HousekeepingOutboxService outboxService,
                               HousekeepingOutboxRepository outboxRepository,
                               HousekeepingInboxRepository inboxRepository,
                               MaintenanceCleanupRequirementRepository requirementRepository,
                               MaintenanceInternalClient maintenanceInternalClient,
                               HousekeepingInboxService inboxService) {
        this.roomStateRepository = roomStateRepository;
        this.cleaningTaskRepository = cleaningTaskRepository;
        this.turnoverProcessor = turnoverProcessor;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
        this.inboxRepository = inboxRepository;
        this.requirementRepository = requirementRepository;
        this.maintenanceInternalClient = maintenanceInternalClient;
        this.inboxService = inboxService;
    }

    public Map<String, Object> reportDefect(String roomNumber, ReportDefectRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.trim().length() > 128) {
            throw new HotelValidationException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required and must be between 1 and 128 characters");
        }
        validateRoomNumber(roomNumber);
        if (request == null || request.getDescription() == null || request.getDescription().isBlank()) {
            throw new HotelValidationException("Issue description is required");
        }
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            throw new HotelValidationException("Priority is required");
        }

        String trimmedRoom = roomNumber.trim();
        Optional<CleaningTaskEntity> activeTaskOpt = cleaningTaskRepository.findActiveTaskForRoom(trimmedRoom);
        String sourceRefId = activeTaskOpt.map(t -> t.getId().toString()).orElse(null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("roomNumber", trimmedRoom);
        body.put("description", request.getDescription().trim());
        body.put("priority", request.getPriority().trim().toUpperCase(Locale.ROOT));
        body.put("source", "HOUSEKEEPING");
        if (sourceRefId != null) {
            body.put("sourceReferenceId", sourceRefId);
        }

        // Call maintenance-service OUTSIDE Housekeeping DB transaction
        return maintenanceInternalClient.createInternalIssue(idempotencyKey.trim(), body);
    }

    @RabbitListener(queues = RabbitConfig.HOUSEKEEPING_ROOM_VACATED_QUEUE)
    public void onRoomVacated(EventEnvelope<RoomVacatedPayload> envelope) {
        RoomVacatedPayload payload = envelope.payload();
        if (payload == null) {
            log.error("Received room.vacated event with null payload, ignoring");
            return;
        }
        try {
            processRoomVacated(envelope);
        } catch (HotelValidationException ex) {
            log.error("Validation error processing room.vacated for room {}: {}",
                    payload.roomNumber(), ex.getMessage());
        }
    }

    public static final String TURNOVER_CORRELATION_CONSTRAINT = "uq_cleaning_tasks_correlation";

    public boolean processRoomVacated(EventEnvelope<RoomVacatedPayload> envelope) {
        RoomVacatedPayload payload = envelope.payload();
        validateRoomNumber(payload.roomNumber());
        validateCorrelationId(envelope.correlationId());

        String trimmedRoom = payload.roomNumber().trim();
        String trimmedCorrelation = envelope.correlationId().trim();

        try {
            return turnoverProcessor.processVacatedWithInbox(envelope, cleaners);
        } catch (DataIntegrityViolationException ex) {
            // Only the recognized turnover-correlation conflict may enter T2; unrelated violations must propagate
            if (!isTurnoverCorrelationViolation(ex)) {
                throw ex;
            }
            boolean shouldRethrow = turnoverProcessor.classifyAndResolveConflict(envelope, trimmedRoom, trimmedCorrelation);
            if (shouldRethrow) {
                throw ex;
            }
            return false;
        }
    }

    private boolean isTurnoverCorrelationViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null && constraintName.toLowerCase(Locale.ROOT).contains(TURNOVER_CORRELATION_CONSTRAINT)) {
                    return true;
                }
            }
            String msg = cause.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains(TURNOVER_CORRELATION_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Transactional
    public void processWorkCompletedWithInbox(EventEnvelope<?> envelope, MaintenanceWorkCompletedPayload payload) {
        HousekeepingInboxService.InboxResult inboxResult = inboxService.registerEvent(envelope);
        if (inboxResult == HousekeepingInboxService.InboxResult.DUPLICATE || inboxResult == HousekeepingInboxService.InboxResult.CONFLICT) {
            log.info("Durable inbox duplicate/conflict for eventId {}; skipping business mutation", envelope.eventId());
            return;
        }

        if (!payload.requiresCleaning()) {
            log.info("Maintenance work for issue {} in room {} completed without requiring cleaning; registered in inbox",
                    payload.issueId(), payload.roomNumber());
            return;
        }

        executeWorkCompletedMutation(payload);
    }

    @Transactional
    public void executeWorkCompletedMutation(MaintenanceWorkCompletedPayload payload) {
        String trimmedRoom = payload.roomNumber().trim();
        Instant normCompletedAt = CanonicalTime.normalize(payload.completedAt());
        Instant now = CanonicalTime.now();

        // Check logical duplicate or conflict
        Optional<MaintenanceCleanupRequirementEntity> existingReqOpt = requirementRepository.findById(payload.issueId());
        if (existingReqOpt.isPresent()) {
            MaintenanceCleanupRequirementEntity existing = existingReqOpt.get();
            Instant existingCompletedAt = CanonicalTime.normalize(existing.getMaintenanceCompletedAt());
            if (existing.getRoomNumber().trim().equals(trimmedRoom) && existingCompletedAt != null && existingCompletedAt.equals(normCompletedAt)) {
                log.info("Logical duplicate maintenance requirement for issueId {}", payload.issueId());
                return;
            } else {
                log.error("CRITICAL_INTEGRITY_MISMATCH: Requirement exists for issueId {} with conflicting room {}/completedAt {}",
                        payload.issueId(), existing.getRoomNumber(), existing.getMaintenanceCompletedAt());
                return;
            }
        }

        // Global lock order: room_states -> cleaning_tasks -> maintenance_cleanup_requirements
        roomStateRepository.insertIfAbsent(trimmedRoom);
        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + trimmedRoom));

        Optional<CleaningTaskEntity> activeTaskOpt = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom);

        // Insert requirement row
        MaintenanceCleanupRequirementEntity req = new MaintenanceCleanupRequirementEntity(
                payload.issueId(),
                trimmedRoom,
                normCompletedAt,
                now
        );
        requirementRepository.save(req);

        // Apply Task Dominance: TURNOVER > MAINTENANCE > MANUAL
        if (activeTaskOpt.isEmpty()) {
            String cleaner = assignCleaner(trimmedRoom);
            CleaningTaskEntity newTask = new CleaningTaskEntity(
                    UUID.randomUUID(),
                    trimmedRoom,
                    TaskSource.MAINTENANCE,
                    null,
                    cleaner,
                    now
            );
            newTask.setSourceReferenceId(payload.issueId().toString());
            cleaningTaskRepository.save(newTask);

            boolean statusChanged = room.getHousekeepingStatus() != HousekeepingStatus.DIRTY;
            room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
            room.setCleanSince(null);
            room.setStatusChangedAt(now);
            roomStateRepository.save(room);

            if (statusChanged) {
                enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, null, now);
            }
            log.info("Created new MAINTENANCE WAITING cleaning task for room {}", trimmedRoom);
        } else {
            CleaningTaskEntity activeTask = activeTaskOpt.get();
            TaskSource taskSource = activeTask.getTaskSource();
            CleaningStatus taskStatus = activeTask.getTaskStatus();

            if (taskSource == TaskSource.MANUAL) {
                if (taskStatus == CleaningStatus.WAITING) {
                    activeTask.setTaskSource(TaskSource.MAINTENANCE);
                    activeTask.setSourceReferenceId(payload.issueId().toString());
                    cleaningTaskRepository.save(activeTask);
                    log.info("Promoted MANUAL WAITING task for room {} to MAINTENANCE", trimmedRoom);
                } else if (taskStatus == CleaningStatus.CLEANING) {
                    Instant startedAt = CanonicalTime.normalize(activeTask.getStartedAt());
                    if (startedAt != null && !startedAt.isBefore(normCompletedAt)) {
                        // startedAt >= completedAt: current cleaning covers this work!
                        activeTask.setTaskSource(TaskSource.MAINTENANCE);
                        activeTask.setSourceReferenceId(payload.issueId().toString());
                        cleaningTaskRepository.save(activeTask);
                        log.info("Promoted MANUAL CLEANING task for room {} to MAINTENANCE; startedAt >= completedAt, preserved CLEANING", trimmedRoom);
                    } else {
                        // startedAt < completedAt: reset cleaning
                        activeTask.setTaskSource(TaskSource.MAINTENANCE);
                        activeTask.setTaskStatus(CleaningStatus.WAITING);
                        activeTask.setStartedAt(null);
                        activeTask.setSourceReferenceId(payload.issueId().toString());
                        cleaningTaskRepository.save(activeTask);

                        boolean statusChanged = room.getHousekeepingStatus() != HousekeepingStatus.DIRTY;
                        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
                        room.setCleanSince(null);
                        room.setStatusChangedAt(now);
                        roomStateRepository.save(room);

                        if (statusChanged) {
                            enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, null, now);
                        }
                        log.info("Reset MANUAL CLEANING task for room {} to MAINTENANCE WAITING; startedAt < completedAt", trimmedRoom);
                    }
                }
            } else if (taskSource == TaskSource.TURNOVER) {
                // TURNOVER dominates! Source remains TURNOVER.
                if (taskStatus == CleaningStatus.CLEANING) {
                    Instant startedAt = CanonicalTime.normalize(activeTask.getStartedAt());
                    if (startedAt != null && startedAt.isBefore(normCompletedAt)) {
                        activeTask.setTaskStatus(CleaningStatus.WAITING);
                        activeTask.setStartedAt(null);
                        cleaningTaskRepository.save(activeTask);

                        boolean statusChanged = room.getHousekeepingStatus() != HousekeepingStatus.DIRTY;
                        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
                        room.setCleanSince(null);
                        room.setStatusChangedAt(now);
                        roomStateRepository.save(room);

                        if (statusChanged) {
                            enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, activeTask.getTurnoverCorrelationId(), now);
                        }
                        log.info("Reset TURNOVER CLEANING task for room {} to WAITING; startedAt < completedAt", trimmedRoom);
                    }
                }
            } else if (taskSource == TaskSource.MAINTENANCE) {
                if (taskStatus == CleaningStatus.CLEANING) {
                    Instant startedAt = CanonicalTime.normalize(activeTask.getStartedAt());
                    if (startedAt != null && startedAt.isBefore(normCompletedAt)) {
                        activeTask.setTaskStatus(CleaningStatus.WAITING);
                        activeTask.setStartedAt(null);
                        cleaningTaskRepository.save(activeTask);

                        boolean statusChanged = room.getHousekeepingStatus() != HousekeepingStatus.DIRTY;
                        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
                        room.setCleanSince(null);
                        room.setStatusChangedAt(now);
                        roomStateRepository.save(room);

                        if (statusChanged) {
                            enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, null, now);
                        }
                        log.info("Reset MAINTENANCE CLEANING task for room {} to WAITING; startedAt < completedAt", trimmedRoom);
                    }
                }
            }
        }
    }

    @Transactional
    public CleaningTask addToQueue(String roomNumber) {
        return addToQueue(roomNumber, null);
    }

    @Transactional
    public CleaningTask addToQueue(String roomNumber, String correlationId) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        roomStateRepository.insertIfAbsent(trimmedRoom);
        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new IllegalStateException("Room state not found immediately after insert: " + trimmedRoom));

        Optional<CleaningTaskEntity> activeTaskOpt = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom);
        if (activeTaskOpt.isPresent()) {
            log.info("Active cleaning task already exists for room {}, returning existing", trimmedRoom);
            return toDomain(activeTaskOpt.get());
        }

        String cleaner = assignCleaner(trimmedRoom);
        Instant now = CanonicalTime.now();
        CleaningTaskEntity newTask = new CleaningTaskEntity(
                UUID.randomUUID(),
                trimmedRoom,
                TaskSource.MANUAL,
                null,
                cleaner,
                now
        );
        cleaningTaskRepository.saveAndFlush(newTask);

        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        room.setActiveTurnoverCorrelationId(null);
        room.setStatusChangedAt(now);
        room.setCleanSince(null);
        roomStateRepository.save(room);

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                        "housekeeping-service",
                        trimmedRoom,
                        null,
                        new RoomHousekeepingStatusChangedPayload(trimmedRoom, HousekeepingStatus.DIRTY.name(), now)
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_HOUSEKEEPING_CHANGED
        );

        return toDomain(newTask);
    }

    @Transactional(readOnly = true)
    public List<CleaningTask> getQueue() {
        return cleaningTaskRepository.findActiveQueue().stream()
                .map(this::toDomain)
                .toList();
    }

    public List<String> getCleaners() {
        return cleaners;
    }

    @Transactional
    public CleaningTask startCleaning(String roomNumber) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        CleaningTaskEntity task = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        if (task.getTaskStatus() == CleaningStatus.CLEANING) {
            throw new HotelValidationException("Task is already in CLEANING state");
        }

        Instant now = CanonicalTime.now();
        task.start(now);
        cleaningTaskRepository.save(task);

        room.setHousekeepingStatus(HousekeepingStatus.CLEANING);
        room.setStatusChangedAt(now);
        roomStateRepository.save(room);

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                        "housekeeping-service",
                        trimmedRoom,
                        task.getTurnoverCorrelationId(),
                        new RoomHousekeepingStatusChangedPayload(trimmedRoom, HousekeepingStatus.CLEANING.name(), now)
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_HOUSEKEEPING_CHANGED
        );

        return toDomain(task);
    }

    @Transactional
    public MarkCleanResult markClean(String roomNumber) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        // Lock order: room_states -> cleaning_tasks -> maintenance_cleanup_requirements
        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        CleaningTaskEntity task = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        if (task.getTaskStatus() == CleaningStatus.WAITING) {
            throw new HotelValidationException("Cannot mark room clean directly from WAITING state; cleaning must be started first");
        }

        Instant startedAt = CanonicalTime.normalize(task.getStartedAt());
        if (startedAt == null) {
            throw new HotelValidationException("Cleaning task has no startedAt timestamp");
        }

        List<MaintenanceCleanupRequirementEntity> pendingReqs = requirementRepository.findPendingForRoomForUpdate(trimmedRoom);
        boolean hasUncovered = pendingReqs.stream().anyMatch(req -> {
            Instant comp = CanonicalTime.normalize(req.getMaintenanceCompletedAt());
            return comp != null && comp.isAfter(startedAt);
        });

        Instant now = CanonicalTime.now();

        if (hasUncovered) {
            // Corrective mutation COMMITTED in DB: task -> WAITING, startedAt -> NULL, room -> DIRTY
            task.setTaskStatus(CleaningStatus.WAITING);
            task.setStartedAt(null);
            cleaningTaskRepository.save(task);

            boolean statusChanged = room.getHousekeepingStatus() != HousekeepingStatus.DIRTY;
            room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
            room.setCleanSince(null);
            room.setStatusChangedAt(now);
            roomStateRepository.save(room);

            if (statusChanged) {
                enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, room.getActiveTurnoverCorrelationId(), now);
            }
            log.warn("markClean rejected: uncovered maintenance requirement for room {}. Committing reset to WAITING/DIRTY.", trimmedRoom);
            return new MarkCleanResult(MarkCleanStatus.RESTART_REQUIRED, toDomain(task));
        }

        // All pending requirements are covered (maintenanceCompletedAt <= startedAt)
        task.complete(now);
        cleaningTaskRepository.save(task);

        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        room.setCleanSince(now);
        room.setStatusChangedAt(now);
        room.setActiveTurnoverCorrelationId(null);
        roomStateRepository.save(room);

        // Satisfy covered requirements ordered deterministically by issueId
        pendingReqs.sort(Comparator.comparing(MaintenanceCleanupRequirementEntity::getIssueId));
        for (MaintenanceCleanupRequirementEntity req : pendingReqs) {
            req.setSatisfiedAt(now);
            requirementRepository.save(req);
        }

        // Outbox INSERT order:
        // FIRST: housekeeping.room.status.changed(CLEAN)
        enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.CLEAN, task.getTurnoverCorrelationId(), now);

        // THEN: housekeeping.maintenance.cleanup.completed for each covered issue
        for (MaintenanceCleanupRequirementEntity req : pendingReqs) {
            enqueueCleanupCompleted(req.getIssueId(), trimmedRoom, now);
        }

        return new MarkCleanResult(MarkCleanStatus.SUCCESS, toDomain(task));
    }

    @Transactional
    public CleaningTask cancel(String roomNumber) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        // 1. Final cancel safety gate: ensure no pending maintenance cleanup requirements
        boolean hasPendingCleanup = requirementRepository.existsByRoomNumberAndSatisfiedAtIsNull(trimmedRoom);
        if (hasPendingCleanup) {
            throw new ConflictException("MAINTENANCE_CLEANUP_REQUIRED",
                    "Room has pending maintenance cleanup requirements; cleaning task cannot be cancelled");
        }

        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        CleaningTaskEntity task = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        if (task.getTaskSource() == TaskSource.TURNOVER) {
            throw new HotelValidationException("Turnover cleaning tasks cannot be cancelled; must be completed to clean");
        }
        if (task.getTaskSource() == TaskSource.MAINTENANCE) {
            throw new ConflictException("MAINTENANCE_CLEANUP_REQUIRED",
                    "Maintenance cleaning tasks cannot be cancelled; must be completed to clean");
        }

        Instant now = CanonicalTime.now();
        CleaningStatus previousStatus = task.getTaskStatus();
        task.cancel(now);
        cleaningTaskRepository.save(task);

        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        room.setCleanSince(null);
        room.setActiveTurnoverCorrelationId(null);

        if (previousStatus == CleaningStatus.CLEANING) {
            room.setStatusChangedAt(now);
            roomStateRepository.save(room);
            enqueueRoomHousekeepingChanged(trimmedRoom, HousekeepingStatus.DIRTY, null, now);
        } else {
            roomStateRepository.save(room);
        }

        return toDomain(task);
    }

    @Transactional
    public Map<String, Object> reset() {
        requirementRepository.deleteAllInBatch();
        cleaningTaskRepository.deleteAllInBatch();
        inboxRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
        Instant now = CanonicalTime.now();
        roomStateRepository.resetAllToClean(now);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Housekeeping queue has been cleared");
        result.put("queueSize", 0);
        return result;
    }

    private void enqueueRoomHousekeepingChanged(String roomNumber, HousekeepingStatus status, String correlationId, Instant changedAt) {
        RoomHousekeepingStatusChangedPayload payload = new RoomHousekeepingStatusChangedPayload(
                roomNumber,
                status.name(),
                CanonicalTime.normalize(changedAt)
        );
        EventEnvelope<RoomHousekeepingStatusChangedPayload> envelope = EventEnvelope.create(
                EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                "housekeeping-service",
                roomNumber,
                correlationId,
                payload
        );
        outboxService.enqueue(envelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_HOUSEKEEPING_CHANGED);
    }

    private void enqueueCleanupCompleted(UUID issueId, String roomNumber, Instant cleanedAt) {
        MaintenanceCleanupCompletedPayload payload = new MaintenanceCleanupCompletedPayload(
                issueId,
                roomNumber,
                CanonicalTime.normalize(cleanedAt)
        );
        EventEnvelope<MaintenanceCleanupCompletedPayload> envelope = EventEnvelope.create(
                EventTypes.MAINTENANCE_CLEANUP_COMPLETED,
                "housekeeping-service",
                roomNumber,
                issueId.toString(),
                payload
        );
        outboxService.enqueue(envelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.MAINTENANCE_CLEANUP_COMPLETED);
    }

    private String assignCleaner(String roomNumber) {
        if (!cleaners.isEmpty()) {
            int index = Math.floorMod(roomNumber.hashCode(), cleaners.size());
            return cleaners.get(index);
        }
        return "Unassigned";
    }

    private CleaningTask toDomain(CleaningTaskEntity entity) {
        Instant updatedAt = switch (entity.getTaskStatus()) {
            case WAITING -> entity.getCreatedAt();
            case CLEANING -> entity.getStartedAt() != null ? entity.getStartedAt() : entity.getCreatedAt();
            case CLEAN -> entity.getCompletedAt() != null ? entity.getCompletedAt() : entity.getCreatedAt();
            case CANCELLED -> entity.getCancelledAt() != null ? entity.getCancelledAt() : entity.getCreatedAt();
        };
        return new CleaningTask(
                entity.getRoomNumber(),
                entity.getTurnoverCorrelationId(),
                entity.getTaskStatus(),
                entity.getCreatedAt(),
                updatedAt,
                entity.getAssignedCleaner()
        );
    }

    private void validateRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (roomNumber.trim().length() > 16) {
            throw new HotelValidationException("Room number cannot exceed 16 characters");
        }
    }

    private void validateCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new HotelValidationException("Correlation ID is required for turnover");
        }
        if (correlationId.trim().length() > 64) {
            throw new HotelValidationException("Correlation ID cannot exceed 64 characters");
        }
    }
}
