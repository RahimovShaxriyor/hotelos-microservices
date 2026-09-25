package com.hotelos.housekeeping.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.RoomHousekeepingStatusChangedPayload;
import com.hotelos.common.event.payload.RoomVacatedPayload;
import com.hotelos.housekeeping.config.RabbitConfig;
import com.hotelos.housekeeping.domain.CleaningStatus;
import com.hotelos.housekeeping.domain.CleaningTask;
import com.hotelos.housekeeping.domain.HousekeepingStatus;
import com.hotelos.housekeeping.domain.TaskSource;
import com.hotelos.housekeeping.exception.HotelValidationException;
import com.hotelos.housekeeping.outbox.HousekeepingOutboxService;
import com.hotelos.housekeeping.persistence.entity.CleaningTaskEntity;
import com.hotelos.housekeeping.persistence.entity.RoomStateEntity;
import com.hotelos.housekeeping.persistence.repository.CleaningTaskRepository;
import com.hotelos.housekeeping.persistence.repository.HousekeepingInboxRepository;
import com.hotelos.housekeeping.persistence.repository.HousekeepingOutboxRepository;
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
    private final List<String> cleaners = List.of("Cleaner-1", "Cleaner-2", "Cleaner-3");

    public HousekeepingService(RoomStateRepository roomStateRepository,
                               CleaningTaskRepository cleaningTaskRepository,
                               TurnoverProcessor turnoverProcessor,
                               HousekeepingOutboxService outboxService,
                               HousekeepingOutboxRepository outboxRepository,
                               HousekeepingInboxRepository inboxRepository) {
        this.roomStateRepository = roomStateRepository;
        this.cleaningTaskRepository = cleaningTaskRepository;
        this.turnoverProcessor = turnoverProcessor;
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
        this.inboxRepository = inboxRepository;
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
            // Transaction T1 rolled back completely (inbox row uncommitted).
            // Transaction T2: Classify conflict and persist inbox record if duplicate/conflict.
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
        Instant now = Instant.now();
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

        Instant now = Instant.now();
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
    public CleaningTask markClean(String roomNumber) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        CleaningTaskEntity task = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        if (task.getTaskStatus() == CleaningStatus.WAITING) {
            throw new HotelValidationException("Cannot mark room clean directly from WAITING state; cleaning must be started first");
        }

        String correlationId = task.getTurnoverCorrelationId();
        Instant now = Instant.now();
        task.complete(now);
        cleaningTaskRepository.save(task);

        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        room.setCleanSince(now);
        room.setStatusChangedAt(now);
        room.setActiveTurnoverCorrelationId(null);
        roomStateRepository.save(room);

        outboxService.enqueue(
                EventEnvelope.create(
                        EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                        "housekeeping-service",
                        trimmedRoom,
                        correlationId,
                        new RoomHousekeepingStatusChangedPayload(trimmedRoom, HousekeepingStatus.CLEAN.name(), now)
                ),
                MessagingConstants.HOTEL_EXCHANGE,
                RoutingKeys.ROOM_HOUSEKEEPING_CHANGED
        );

        return toDomain(task);
    }

    @Transactional
    public CleaningTask cancel(String roomNumber) {
        validateRoomNumber(roomNumber);
        String trimmedRoom = roomNumber.trim();

        RoomStateEntity room = roomStateRepository.findByRoomNumberForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        CleaningTaskEntity task = cleaningTaskRepository.findActiveTaskForUpdate(trimmedRoom)
                .orElseThrow(() -> new HotelValidationException("No housekeeping task exists for room " + trimmedRoom));

        if (task.getTaskSource() == TaskSource.TURNOVER) {
            throw new HotelValidationException("Turnover cleaning tasks cannot be cancelled; must be completed to clean");
        }

        Instant now = Instant.now();
        CleaningStatus previousStatus = task.getTaskStatus();
        task.cancel(now);
        cleaningTaskRepository.save(task);

        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        room.setCleanSince(null);
        room.setActiveTurnoverCorrelationId(null);

        if (previousStatus == CleaningStatus.CLEANING) {
            room.setStatusChangedAt(now);
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
        } else {
            roomStateRepository.save(room);
        }

        return toDomain(task);
    }

    @Transactional
    public Map<String, Object> reset() {
        cleaningTaskRepository.deleteAllInBatch();
        inboxRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
        Instant now = Instant.now();
        roomStateRepository.resetAllToClean(now);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Housekeeping queue has been cleared");
        result.put("queueSize", 0);
        return result;
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
