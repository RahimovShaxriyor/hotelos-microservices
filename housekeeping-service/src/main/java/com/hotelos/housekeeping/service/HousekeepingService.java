package com.hotelos.housekeeping.service;

import com.hotelos.housekeeping.config.RabbitConfig;
import com.hotelos.housekeeping.domain.CleaningTask;
import com.hotelos.housekeeping.event.RoomStatusChangedEvent;
import com.hotelos.housekeeping.event.RoomVacatedEvent;
import com.hotelos.housekeeping.exception.HotelValidationException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class HousekeepingService {
    private final Queue<CleaningTask> cleaningQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CleaningTask> tasksByRoom = new ConcurrentHashMap<>();
    private final List<String> cleaners = List.of("Cleaner-1", "Cleaner-2", "Cleaner-3");
    private final RabbitTemplate rabbitTemplate;

    public HousekeepingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.HOUSEKEEPING_ROOM_VACATED_QUEUE)
    public void onRoomVacated(RoomVacatedEvent event) {
        addToQueue(event.roomNumber());
    }

    public synchronized CleaningTask addToQueue(String roomNumber) {
        validateRoomNumber(roomNumber);
        CleaningTask existing = tasksByRoom.get(roomNumber);
        if (existing != null && existing.getStatus().name().equals("WAITING")) {
            return existing;
        }
        CleaningTask task = new CleaningTask(roomNumber);
        task.assignCleaner(cleaners.get(Math.abs(roomNumber.hashCode()) % cleaners.size()));
        tasksByRoom.put(roomNumber, task);
        cleaningQueue.add(task);
        return task;
    }

    public List<CleaningTask> getQueue() {
        return new ArrayList<>(cleaningQueue);
    }

    public List<String> getCleaners() {
        return cleaners;
    }

    public CleaningTask startCleaning(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.start();
        publishStatus(roomNumber, "CLEANING");
        return task;
    }

    public CleaningTask markClean(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.complete();
        cleaningQueue.removeIf(item -> item.getRoomNumber().equals(roomNumber));
        publishStatus(roomNumber, "CLEAN");
        return task;
    }

    public CleaningTask cancel(String roomNumber) {
        CleaningTask task = findTask(roomNumber);
        task.cancel();
        cleaningQueue.removeIf(item -> item.getRoomNumber().equals(roomNumber));
        return task;
    }

    public synchronized Map<String, Object> reset() {
        cleaningQueue.clear();
        tasksByRoom.clear();
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

    private void publishStatus(String roomNumber, String status) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROOM_STATUS_CHANGED,
                new RoomStatusChangedEvent(roomNumber, status, Instant.now()));
    }
}
