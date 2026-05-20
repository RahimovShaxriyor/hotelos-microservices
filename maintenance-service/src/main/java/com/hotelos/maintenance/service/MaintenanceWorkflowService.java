package com.hotelos.maintenance.service;

import com.hotelos.maintenance.config.RabbitConfig;
import com.hotelos.maintenance.domain.*;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.event.MaintenanceIssueEvent;
import com.hotelos.maintenance.event.RoomStatusChangedEvent;
import com.hotelos.maintenance.exception.HotelValidationException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MaintenanceWorkflowService {
    private final Map<String, MaintenanceIssue> issues = new ConcurrentHashMap<>();
    private final PriorityQueue<MaintenanceIssue> priorityQueue = new PriorityQueue<>(
            Comparator.comparingInt((MaintenanceIssue issue) -> issue.getPriority().getRank())
                    .thenComparing(MaintenanceIssue::getCreatedAt));
    private final Queue<String> availableTechnicians = new ArrayDeque<>(List.of("Tech-1", "Tech-2"));
    private final List<String> allTechnicians = List.of("Tech-1", "Tech-2");
    private final RabbitTemplate rabbitTemplate;

    public MaintenanceWorkflowService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public synchronized MaintenanceIssue report(CreateIssueRequest request) {
        validate(request);
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        MaintenanceIssue issue = new MaintenanceIssue(request.getRoomNumber(), request.getDescription(), priority);
        issues.put(issue.getIssueId(), issue);
        priorityQueue.add(issue);
        publishRoomStatus(issue.getRoomNumber(), "MAINTENANCE");
        assignNextIfPossible();
        publishIssue(issue);
        return issue;
    }

    public MaintenanceIssue getIssue(String issueId) {
        MaintenanceIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new HotelValidationException("Unknown maintenance issue ID: " + issueId);
        }
        return issue;
    }

    public synchronized MaintenanceIssue resolve(String issueId) {
        MaintenanceIssue issue = getIssue(issueId);
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new HotelValidationException("Cancelled maintenance issue cannot be resolved");
        }
        issue.resolve();
        releaseTechnician(issue);
        publishIssue(issue);
        publishRoomStatus(issue.getRoomNumber(), "CLEAN");
        assignNextIfPossible();
        return issue;
    }

    public synchronized MaintenanceIssue cancel(String issueId) {
        MaintenanceIssue issue = getIssue(issueId);
        priorityQueue.remove(issue);
        issue.cancel();
        releaseTechnician(issue);
        publishIssue(issue);
        assignNextIfPossible();
        return issue;
    }

    public synchronized List<MaintenanceIssue> processNext() {
        assignNextIfPossible();
        return getPriorityQueueSnapshot();
    }

    public List<String> getTechnicians() {
        return allTechnicians;
    }

    public List<MaintenanceIssue> getIssues() {
        return issues.values().stream().sorted(Comparator.comparing(MaintenanceIssue::getCreatedAt)).toList();
    }

    public List<MaintenanceIssue> getPriorityQueueSnapshot() {
        return priorityQueue.stream()
                .sorted(Comparator.comparingInt((MaintenanceIssue issue) -> issue.getPriority().getRank())
                        .thenComparing(MaintenanceIssue::getCreatedAt))
                .toList();
    }

    public synchronized Map<String, Object> reset() {
        issues.clear();
        priorityQueue.clear();
        availableTechnicians.clear();
        availableTechnicians.addAll(allTechnicians);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Maintenance issues and priority queue have been cleared");
        result.put("issues", issues.size());
        result.put("availableTechnicians", new ArrayList<>(availableTechnicians));
        return result;
    }

    private void releaseTechnician(MaintenanceIssue issue) {
        if (issue.getAssignedTechnician() != null && !availableTechnicians.contains(issue.getAssignedTechnician())) {
            availableTechnicians.add(issue.getAssignedTechnician());
        }
    }

    private void assignNextIfPossible() {
        while (!priorityQueue.isEmpty() && !availableTechnicians.isEmpty()) {
            MaintenanceIssue issue = priorityQueue.poll();
            if (issue.getStatus() == IssueStatus.OPEN) {
                String technician = availableTechnicians.poll();
                issue.assign(technician);
                publishIssue(issue);
            }
        }
    }

    private void validate(CreateIssueRequest request) {
        if (request == null || request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new HotelValidationException("Issue description is required");
        }
        try {
            IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new HotelValidationException("Priority must be CRITICAL, HIGH, NORMAL or LOW");
        }
    }

    private void publishIssue(MaintenanceIssue issue) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.MAINTENANCE_ISSUE_UPDATED,
                new MaintenanceIssueEvent(issue.getIssueId(), issue.getRoomNumber(), issue.getPriority().name(),
                        issue.getStatus().name(), issue.getAssignedTechnician(), Instant.now()));
    }

    private void publishRoomStatus(String roomNumber, String status) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROOM_STATUS_CHANGED,
                new RoomStatusChangedEvent(roomNumber, status, Instant.now()));
    }
}
