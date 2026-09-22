package com.hotelos.maintenance.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.MaintenanceIssueUpdatedPayload;
import com.hotelos.common.event.payload.RoomEngineeringStatusChangedPayload;
import com.hotelos.maintenance.domain.*;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.exception.HotelValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MaintenanceWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceWorkflowService.class);

    private final Object queueLock = new Object();
    private static final Comparator<MaintenanceIssue> PRIORITY_COMPARATOR =
            Comparator.comparingInt((MaintenanceIssue issue) -> issue.getPriority().getRank())
                    .thenComparing(MaintenanceIssue::getCreatedAt);

    private final Map<String, MaintenanceIssue> issues = new ConcurrentHashMap<>();
    private final PriorityQueue<MaintenanceIssue> priorityQueue = new PriorityQueue<>(PRIORITY_COMPARATOR);
    private final Queue<String> availableTechnicians = new ArrayDeque<>(List.of("Tech-1", "Tech-2"));
    private final List<String> allTechnicians = List.of("Tech-1", "Tech-2");
    private final RabbitTemplate rabbitTemplate;

    public MaintenanceWorkflowService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public MaintenanceIssue report(CreateIssueRequest request) {
        validate(request);
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        MaintenanceIssue issue = new MaintenanceIssue(request.getRoomNumber(), request.getDescription(), priority);
        issues.put(issue.getIssueId(), issue);
        List<MaintenanceIssue> assignedIssues = new ArrayList<>();
        synchronized (queueLock) {
            priorityQueue.add(issue);
            assignNextIfPossible(assignedIssues);
        }
        publishEngineeringStatus(issue.getRoomNumber(), EngineeringStatus.OUT_OF_ORDER);
        publishIssue(issue);
        for (MaintenanceIssue assigned : assignedIssues) {
            publishIssue(assigned);
        }
        return issue;
    }

    public MaintenanceIssue getIssue(String issueId) {
        MaintenanceIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new HotelValidationException("Unknown maintenance issue ID: " + issueId);
        }
        return issue;
    }

    public MaintenanceIssue resolve(String issueId) {
        MaintenanceIssue issue = getIssue(issueId);
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new HotelValidationException("Cancelled maintenance issue cannot be resolved");
        }
        List<MaintenanceIssue> assignedIssues = new ArrayList<>();
        synchronized (queueLock) {
            issue.resolve();
            releaseTechnician(issue);
            assignNextIfPossible(assignedIssues);
        }
        publishIssue(issue);
        publishEngineeringStatus(issue.getRoomNumber(), EngineeringStatus.OPERATIONAL);
        for (MaintenanceIssue assigned : assignedIssues) {
            publishIssue(assigned);
        }
        return issue;
    }

    public MaintenanceIssue cancel(String issueId) {
        MaintenanceIssue issue = getIssue(issueId);
        List<MaintenanceIssue> assignedIssues = new ArrayList<>();
        synchronized (queueLock) {
            priorityQueue.remove(issue);
            issue.cancel();
            releaseTechnician(issue);
            assignNextIfPossible(assignedIssues);
        }
        publishIssue(issue);
        for (MaintenanceIssue assigned : assignedIssues) {
            publishIssue(assigned);
        }
        return issue;
    }

    public List<MaintenanceIssue> processNext() {
        List<MaintenanceIssue> assignedIssues = new ArrayList<>();
        List<MaintenanceIssue> snapshot;
        synchronized (queueLock) {
            assignNextIfPossible(assignedIssues);
            snapshot = priorityQueue.stream()
                    .sorted(PRIORITY_COMPARATOR)
                    .toList();
        }
        for (MaintenanceIssue assigned : assignedIssues) {
            publishIssue(assigned);
        }
        return snapshot;
    }

    public List<String> getTechnicians() {
        return allTechnicians;
    }

    public List<MaintenanceIssue> getIssues() {
        return issues.values().stream().sorted(Comparator.comparing(MaintenanceIssue::getCreatedAt)).toList();
    }

    public List<MaintenanceIssue> getPriorityQueueSnapshot() {
        synchronized (queueLock) {
            return priorityQueue.stream()
                    .sorted(PRIORITY_COMPARATOR)
                    .toList();
        }
    }

    public Map<String, Object> reset() {
        issues.clear();
        synchronized (queueLock) {
            priorityQueue.clear();
            availableTechnicians.clear();
            availableTechnicians.addAll(allTechnicians);
        }
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

    private void assignNextIfPossible(List<MaintenanceIssue> newlyAssigned) {
        while (!priorityQueue.isEmpty() && !availableTechnicians.isEmpty()) {
            MaintenanceIssue peek = priorityQueue.peek();
            if (peek == null) {
                break;
            }
            if (peek.getStatus() != IssueStatus.OPEN) {
                priorityQueue.poll();
                continue;
            }
            String technician = availableTechnicians.poll();
            if (technician == null) {
                log.info("No technicians currently available; issue {} remains queued", peek.getIssueId());
                break;
            }
            MaintenanceIssue issue = priorityQueue.poll();
            if (issue == null) {
                availableTechnicians.offer(technician);
                break;
            }
            issue.assign(technician);
            log.info("Assigned technician {} to issue {} for room {}", technician, issue.getIssueId(), issue.getRoomNumber());
            newlyAssigned.add(issue);
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
        MaintenanceIssueUpdatedPayload payload = new MaintenanceIssueUpdatedPayload(
                issue.getIssueId(),
                issue.getRoomNumber(),
                issue.getPriority().name(),
                issue.getStatus().name(),
                issue.getAssignedTechnician(),
                Instant.now()
        );
        EventEnvelope<MaintenanceIssueUpdatedPayload> envelope = EventEnvelope.create(
                EventTypes.MAINTENANCE_ISSUE_UPDATED,
                "maintenance-service",
                issue.getIssueId(),
                null,
                payload
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.MAINTENANCE_ISSUE_UPDATED, envelope);
    }

    private void publishEngineeringStatus(String roomNumber, EngineeringStatus status) {
        RoomEngineeringStatusChangedPayload payload = new RoomEngineeringStatusChangedPayload(roomNumber, status.name(), Instant.now());
        EventEnvelope<RoomEngineeringStatusChangedPayload> envelope = EventEnvelope.create(
                EventTypes.ROOM_ENGINEERING_CHANGED,
                "maintenance-service",
                roomNumber,
                null,
                payload
        );
        rabbitTemplate.convertAndSend(MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_ENGINEERING_CHANGED, envelope);
    }
}
