package com.hotelos.maintenance.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.MaintenanceIssueUpdatedPayload;
import com.hotelos.common.event.payload.RoomEngineeringStatusChangedPayload;
import com.hotelos.maintenance.domain.EngineeringStatus;
import com.hotelos.maintenance.domain.IssuePriority;
import com.hotelos.maintenance.domain.IssueStatus;
import com.hotelos.maintenance.domain.MaintenanceIssue;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.exception.HotelValidationException;
import com.hotelos.maintenance.outbox.MaintenanceOutboxService;
import com.hotelos.maintenance.persistence.entity.MaintenanceIssueEntity;
import com.hotelos.maintenance.persistence.entity.RoomStateEntity;
import com.hotelos.maintenance.persistence.entity.TechnicianEntity;
import com.hotelos.maintenance.persistence.repository.MaintenanceIssueRepository;
import com.hotelos.maintenance.persistence.repository.MaintenanceOutboxRepository;
import com.hotelos.maintenance.persistence.repository.RoomStateRepository;
import com.hotelos.maintenance.persistence.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MaintenanceTransactionService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceTransactionService.class);

    private final TechnicianRepository technicianRepository;
    private final RoomStateRepository roomStateRepository;
    private final MaintenanceIssueRepository issueRepository;
    private final MaintenanceOutboxRepository outboxRepository;
    private final MaintenanceOutboxService outboxService;

    public MaintenanceTransactionService(TechnicianRepository technicianRepository,
                                        RoomStateRepository roomStateRepository,
                                        MaintenanceIssueRepository issueRepository,
                                        MaintenanceOutboxRepository outboxRepository,
                                        MaintenanceOutboxService outboxService) {
        this.technicianRepository = technicianRepository;
        this.roomStateRepository = roomStateRepository;
        this.issueRepository = issueRepository;
        this.outboxRepository = outboxRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public MutationResult executeReport(CreateIssueRequest request) {
        Instant now = Instant.now();
        String roomNumber = request.getRoomNumber().trim();
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));

        // Global lock order: room_states -> maintenance_issues -> technicians
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        UUID issueId = UUID.randomUUID();
        MaintenanceIssueEntity issueEntity = new MaintenanceIssueEntity(issueId, roomNumber, request.getDescription().trim(), priority, now);
        issueEntity = issueRepository.save(issueEntity);

        if (roomState.getEngineeringStatus() != EngineeringStatus.OUT_OF_ORDER) {
            roomState.setEngineeringStatus(EngineeringStatus.OUT_OF_ORDER);
            roomState.setStatusChangedAt(now);
            roomState.setUpdatedAt(now);
            roomStateRepository.save(roomState);
            enqueueRoomEngineeringChanged(roomNumber, EngineeringStatus.OUT_OF_ORDER, now);
        }

        enqueueIssueUpdated(issueEntity.getId().toString(), roomNumber, priority.name(), IssueStatus.OPEN.name(), null, now);

        // Try claim technician under lock
        Optional<TechnicianEntity> techOpt = technicianRepository.claimNextAvailableTechnician();
        if (techOpt.isPresent()) {
            TechnicianEntity tech = techOpt.get();
            issueEntity.assign(tech.getName(), now);
            tech.setState("BUSY");
            tech.setAvailabilitySequence(null);
            tech.setUpdatedAt(now);
            technicianRepository.save(tech);
            issueRepository.save(issueEntity);
            enqueueIssueUpdated(issueEntity.getId().toString(), roomNumber, priority.name(), IssueStatus.ASSIGNED.name(), tech.getName(), now);
            log.info("Assigned technician {} to newly reported issue {} for room {}", tech.getName(), issueEntity.getId(), roomNumber);
        }

        return new MutationResult(MaintenanceIssue.fromEntity(issueEntity));
    }

    @Transactional
    public ProcessNextResult executeProcessNext() {
        Instant now = Instant.now();

        // Global lock order: maintenance_issues -> technicians
        // 1. Claim next OPEN issue
        Optional<MaintenanceIssueEntity> issueOpt = issueRepository.claimNextOpenIssue();
        if (issueOpt.isEmpty()) {
            return new ProcessNextResult(null, loadQueueSnapshot());
        }

        // 2. Claim next AVAILABLE technician
        Optional<TechnicianEntity> techOpt = technicianRepository.claimNextAvailableTechnician();
        if (techOpt.isEmpty()) {
            // No technician available: do not mutate issue, commit and release issue lock cleanly
            log.info("No available technician for open issue {}; leaving in queue", issueOpt.get().getId());
            return new ProcessNextResult(null, loadQueueSnapshot());
        }

        // 3. Assign
        MaintenanceIssueEntity issue = issueOpt.get();
        TechnicianEntity tech = techOpt.get();

        issue.assign(tech.getName(), now);
        tech.setState("BUSY");
        tech.setAvailabilitySequence(null);
        tech.setUpdatedAt(now);

        technicianRepository.save(tech);
        issueRepository.save(issue);

        enqueueIssueUpdated(
                issue.getId().toString(),
                issue.getRoomNumber(),
                issue.getPriority().name(),
                IssueStatus.ASSIGNED.name(),
                tech.getName(),
                now
        );

        log.info("ProcessNext assigned technician {} to issue {} for room {}", tech.getName(), issue.getId(), issue.getRoomNumber());
        return new ProcessNextResult(MaintenanceIssue.fromEntity(issue), loadQueueSnapshot());
    }

    @Transactional
    public MutationResult executeResolve(UUID issueId) {
        Instant now = Instant.now();

        // 1. Non-locking lookup to determine issue metadata
        MaintenanceIssueEntity issueMeta = issueRepository.findById(issueId)
                .orElseThrow(() -> new HotelValidationException("Unknown maintenance issue ID: " + issueId));

        String roomNumber = issueMeta.getRoomNumber();

        // 2. Global lock order: room_states -> maintenance_issues -> technicians
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        MaintenanceIssueEntity issue = issueRepository.lockIssue(issueId)
                .orElseThrow(() -> new HotelValidationException("Unknown maintenance issue ID: " + issueId));

        // Terminal status checks
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            log.info("Issue {} already resolved; returning idempotently with 0 events", issueId);
            return new MutationResult(MaintenanceIssue.fromEntity(issue));
        }
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            throw new HotelValidationException("Cancelled maintenance issue cannot be resolved");
        }

        // 3. Resolve issue
        issue.resolve(now);
        issueRepository.save(issue);

        // 4. Release technician if assigned
        String assignedTech = issue.getAssignedTechnician();
        if (assignedTech != null) {
            Long nextSeq = technicianRepository.getNextAvailabilitySequence();
            technicianRepository.releaseTechnician(assignedTech, nextSeq, now);
            log.info("Released technician {} with sequence {}", assignedTech, nextSeq);
        }

        enqueueIssueUpdated(issue.getId().toString(), roomNumber, issue.getPriority().name(), IssueStatus.RESOLVED.name(), assignedTech, now);

        // 5. Update room engineering status if no remaining active issues
        long activeIssuesCount = issueRepository.countActiveIssuesForRoom(roomNumber);
        if (activeIssuesCount == 0 && roomState.getEngineeringStatus() != EngineeringStatus.OPERATIONAL) {
            roomState.setEngineeringStatus(EngineeringStatus.OPERATIONAL);
            roomState.setStatusChangedAt(now);
            roomState.setUpdatedAt(now);
            roomStateRepository.save(roomState);
            enqueueRoomEngineeringChanged(roomNumber, EngineeringStatus.OPERATIONAL, now);
            log.info("Room {} transitioned to OPERATIONAL following resolve of issue {}", roomNumber, issueId);
        }

        return new MutationResult(MaintenanceIssue.fromEntity(issue));
    }

    @Transactional
    public MutationResult executeCancel(UUID issueId) {
        Instant now = Instant.now();

        // 1. Non-locking lookup to determine issue metadata
        MaintenanceIssueEntity issueMeta = issueRepository.findById(issueId)
                .orElseThrow(() -> new HotelValidationException("Unknown maintenance issue ID: " + issueId));

        String roomNumber = issueMeta.getRoomNumber();

        // 2. Global lock order: room_states -> maintenance_issues -> technicians
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        MaintenanceIssueEntity issue = issueRepository.lockIssue(issueId)
                .orElseThrow(() -> new HotelValidationException("Unknown maintenance issue ID: " + issueId));

        // Terminal status checks
        if (issue.getStatus() == IssueStatus.CANCELLED) {
            log.info("Issue {} already cancelled; returning idempotently with 0 events", issueId);
            return new MutationResult(MaintenanceIssue.fromEntity(issue));
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            throw new HotelValidationException("Resolved maintenance issue cannot be cancelled");
        }

        // 3. Cancel issue
        issue.cancel(now);
        issueRepository.save(issue);

        // 4. Release technician if assigned
        String assignedTech = issue.getAssignedTechnician();
        if (assignedTech != null) {
            Long nextSeq = technicianRepository.getNextAvailabilitySequence();
            technicianRepository.releaseTechnician(assignedTech, nextSeq, now);
            log.info("Released technician {} with sequence {}", assignedTech, nextSeq);
        }

        enqueueIssueUpdated(issue.getId().toString(), roomNumber, issue.getPriority().name(), IssueStatus.CANCELLED.name(), assignedTech, now);

        // 5. Update room engineering status if no remaining active issues (INTENTIONAL P1.4 CORRECTION)
        long activeIssuesCount = issueRepository.countActiveIssuesForRoom(roomNumber);
        if (activeIssuesCount == 0 && roomState.getEngineeringStatus() != EngineeringStatus.OPERATIONAL) {
            roomState.setEngineeringStatus(EngineeringStatus.OPERATIONAL);
            roomState.setStatusChangedAt(now);
            roomState.setUpdatedAt(now);
            roomStateRepository.save(roomState);
            enqueueRoomEngineeringChanged(roomNumber, EngineeringStatus.OPERATIONAL, now);
            log.info("Room {} transitioned to OPERATIONAL following cancel of final issue {}", roomNumber, issueId);
        }

        return new MutationResult(MaintenanceIssue.fromEntity(issue));
    }

    @Transactional
    public void executeDevReset() {
        outboxRepository.deleteAllInBatch();
        issueRepository.deleteAll();
        roomStateRepository.deleteAll();
        Instant now = Instant.now();
        technicianRepository.resetTech1(now);
        technicianRepository.resetTech2(now);
        technicianRepository.resetSequence();
        log.info("Dev reset completed: outbox, issues and room states wiped, Tech-1 and Tech-2 reset to AVAILABLE with sequence 1 and 2");
    }

    @Transactional(readOnly = true)
    public List<MaintenanceIssue> loadQueueSnapshot() {
        return issueRepository.findOpenQueue().stream()
                .map(MaintenanceIssue::fromEntity)
                .toList();
    }

    private void enqueueIssueUpdated(String issueId, String roomNumber, String priority,
                                     String status, String assignedTechnician, Instant changedAt) {
        MaintenanceIssueUpdatedPayload payload = new MaintenanceIssueUpdatedPayload(
                issueId,
                roomNumber,
                priority,
                status,
                assignedTechnician,
                changedAt
        );
        EventEnvelope<MaintenanceIssueUpdatedPayload> envelope = EventEnvelope.create(
                EventTypes.MAINTENANCE_ISSUE_UPDATED,
                "maintenance-service",
                issueId,
                null,
                payload
        );
        outboxService.enqueue(envelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.MAINTENANCE_ISSUE_UPDATED);
    }

    private void enqueueRoomEngineeringChanged(String roomNumber, EngineeringStatus status, Instant changedAt) {
        RoomEngineeringStatusChangedPayload payload = new RoomEngineeringStatusChangedPayload(
                roomNumber,
                status.name(),
                changedAt
        );
        EventEnvelope<RoomEngineeringStatusChangedPayload> envelope = EventEnvelope.create(
                EventTypes.ROOM_ENGINEERING_CHANGED,
                "maintenance-service",
                roomNumber,
                null,
                payload
        );
        outboxService.enqueue(envelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.ROOM_ENGINEERING_CHANGED);
    }
}
