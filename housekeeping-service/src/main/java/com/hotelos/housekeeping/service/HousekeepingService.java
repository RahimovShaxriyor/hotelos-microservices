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
import com.hotelos.housekeeping.exception.HotelValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class HousekeepingService {
    private static final Logger log = LoggerFactory.getLogger(HousekeepingService.class);

    private static final int MAX_PROCESSED_TURNOVERS = 1000;

    private final Queue<CleaningTask> cleaningQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CleaningTask> tasksByRoom = new ConcurrentHashMap<>();
    private final Map<String, Instant> processedTurnoverIds = Collections.synchronizedMap(
            new LinkedHashMap<String, Instant>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
                    return size() > MAX_PROCESSED_TURNOVERS;
                }
            }
    );
    private final List<String> cleaners = List.of("Cleaner-1", "Cleaner-2", "Cleaner-3");
    private final RabbitTemplate rabbitTemplate;

    public HousekeepingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.HOUSEKEEPING_ROOM_VACATED_QUEUE)
    public void onRoomVacated(EventEnvelope<RoomVacatedPayload> envelope) {
        RoomVacatedPayload payload = envelope.payload();
        boolean created = processRoomVacated(payload.roomNumber(), envelope.correlationId());
        if (created) {
            publishStatus(payload.roomNumber(), HousekeepingStatus.DIRTY.name(), envelope.correlationId());
        }
    }

    public synchronized boolean processRoomVacated(String roomNumber, String correlationId) {
        validateRoomNumber(roomNumber);
        if (correlationId != null && !correlationId.isBlank()) {
            if (processedTurnoverIds.containsKey(correlationId)) {
                log.info("Turnover {} for room {} has already been processed or is active, ignoring duplicate/stale room.vacated",
                        correlationId, roomNumber);
                return false;
            }
        }
        CleaningTask existing = tasksByRoom.get(roomNumber);
        if (existing != null && (existing.getStatus() == CleaningStatus.WAITING || existing.getStatus() == CleaningStatus.CLEANING)) {
            log.info("Active cleaning task already exists for room {} (status: {}), ignoring duplicate turnover request without marking correlationId processed",
                    roomNumber, existing.getStatus());
            return false;
        }
        CleaningTask task = new CleaningTask(roomNumber, correlationId);
        if (!cleaners.isEmpty()) {
            int index = Math.floorMod(roomNumber.hashCode(), cleaners.size());
            task.assignCleaner(cleaners.get(index));
        } else {
            task.assignCleaner("Unassigned");
        }
        tasksByRoom.put(roomNumber, task);
        cleaningQueue.add(task);

        // ONLY mark correlationId as processed after the turnover is actually accepted into active state
        if (correlationId != null && !correlationId.isBlank()) {
            processedTurnoverIds.put(correlationId, Instant.now());
        }
        return true;
    }

    public synchronized CleaningTask addToQueue(String roomNumber) {
        return addToQueue(roomNumber, null);
    }

    public synchronized CleaningTask addToQueue(String roomNumber, String correlationId) {
        validateRoomNumber(roomNumber);
        if (correlationId != null && !correlationId.isBlank()) {
            if (processedTurnoverIds.containsKey(correlationId)) {
                log.info("Turnover {} for room {} has already been processed, returning existing", correlationId, roomNumber);
                CleaningTask existing = tasksByRoom.get(roomNumber);
                if (existing != null) {
                    return existing;
                }
            }
        }
        CleaningTask existing = tasksByRoom.get(roomNumber);
        if (existing != null && (existing.getStatus() == CleaningStatus.WAITING || existing.getStatus() == CleaningStatus.CLEANING)) {
            log.info("Active cleaning task already exists for room {}, returning existing without marking new correlationId processed", roomNumber);
            return existing;
        }
        CleaningTask task = new CleaningTask(roomNumber, correlationId);
        if (!cleaners.isEmpty()) {
            int index = Math.floorMod(roomNumber.hashCode(), cleaners.size());
            task.assignCleaner(cleaners.get(index));
        } else {
            task.assignCleaner("Unassigned");
        }
        tasksByRoom.put(roomNumber, task);
        cleaningQueue.add(task);
        if (correlationId != null && !correlationId.isBlank()) {
            processedTurnoverIds.put(correlationId, Instant.now());
        }
        return task;
    }

    public List<CleaningTask> getQueue() {
        return new ArrayList<>(cleaningQueue);
    }

    public List<String> getCleaners() {
        return cleaners;
    }

    public synchronized CleaningTask startCleaning(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.start();
        publishStatus(roomNumber, HousekeepingStatus.CLEANING.name(), task.getCorrelationId());
        return task;
    }

    public synchronized CleaningTask markClean(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.complete();
        cleaningQueue.removeIf(item -> item.getRoomNumber().equals(roomNumber));
        tasksByRoom.remove(roomNumber);
        publishStatus(roomNumber, HousekeepingStatus.CLEAN.name(), task.getCorrelationId());
        return task;
    }

    public synchronized CleaningTask cancel(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.cancel();
        cleaningQueue.removeIf(item -> item.getRoomNumber().equals(roomNumber));
        tasksByRoom.remove(roomNumber);
        return task;
    }

    public synchronized Map<String, Object> reset() {
        cleaningQueue.clear();
        tasksByRoom.clear();
        processedTurnoverIds.clear();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Housekeeping queue has been cleared");
        result.put("queueSize", cleaningQueue.size());
        return result;
    }

    private CleaningTask findTask(String roomNumber) {
        CleaningTask task = tasksByRoom.get(roomNumber);
        if (task == null) {
            throw new HotelValidationException("No housekeeping task exists for room " + roomNumber);
        }
        return task;
    }

    private void validateRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
    }

    private void publishStatus(String roomNumber, String status, String correlationId) {
        RoomHousekeepingStatusChangedPayload payload = new RoomHousekeepingStatusChangedPayload(roomNumber, status, Instant.now());
        EventEnvelope<RoomHousekeepingStatusChangedPayload> envelope = EventEnvelope.create(
                EventTypes.ROOM_HOUSEKEEPING_CHANGED,
                "housekeeping-service",
                roomNumber,
                correlationId,
                payload
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_HOUSEKEEPING_CHANGED, envelope);
    }
}
