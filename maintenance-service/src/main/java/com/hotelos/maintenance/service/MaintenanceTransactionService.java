package com.hotelos.maintenance.service;

import com.hotelos.common.event.EventEnvelope;
import com.hotelos.common.event.EventTypes;
import com.hotelos.common.event.MessagingConstants;
import com.hotelos.common.event.RoutingKeys;
import com.hotelos.common.event.payload.MaintenanceIssueUpdatedPayload;
import com.hotelos.common.event.payload.MaintenanceWorkCompletedPayload;
import com.hotelos.common.event.payload.RoomEngineeringStatusChangedPayload;
import com.hotelos.common.util.CanonicalTime;
import com.hotelos.maintenance.domain.EngineeringImpact;
import com.hotelos.maintenance.domain.EngineeringStatus;
import com.hotelos.maintenance.domain.IssuePriority;
import com.hotelos.maintenance.domain.IssueSource;
import com.hotelos.maintenance.domain.IssueStatus;
import com.hotelos.maintenance.domain.MaintenanceIssue;
import com.hotelos.maintenance.dto.CreateInternalIssueRequest;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.dto.ResolveIssueRequest;
import com.hotelos.maintenance.exception.ConflictException;
import com.hotelos.maintenance.exception.HotelValidationException;
import com.hotelos.maintenance.inbox.MaintenanceInboxService;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final MaintenanceInboxService inboxService;

    public MaintenanceTransactionService(TechnicianRepository technicianRepository,
                                         RoomStateRepository roomStateRepository,
                                         MaintenanceIssueRepository issueRepository,
                                         MaintenanceOutboxRepository outboxRepository,
                                         MaintenanceOutboxService outboxService,
                                         MaintenanceInboxService inboxService) {
        this.technicianRepository = technicianRepository;
        this.roomStateRepository = roomStateRepository;
        this.issueRepository = issueRepository;
        this.outboxRepository = outboxRepository;
        this.outboxService = outboxService;
        this.inboxService = inboxService;
    }

    public static String computeFingerprint(IssueSource source, String roomNumber, String description,
                                            IssuePriority priority, EngineeringImpact impact) {
        String raw = "v1:" + source.name() + ":" + roomNumber.trim() + ":" + description.trim() + ":" + priority.name() + ":" + impact.name();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<MaintenanceIssueEntity> findExistingBySourceAndKey(IssueSource source, String idempotencyKey) {
        return issueRepository.findBySourceAndIdempotencyKey(source, idempotencyKey);
    }

    @Transactional
    public MutationResult executeReport(CreateIssueRequest request) {
        Instant now = CanonicalTime.now();
        String roomNumber = request.getRoomNumber().trim();
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));

        // Global lock order: room_states -> maintenance_issues -> technicians
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        UUID issueId = UUID.randomUUID();
        MaintenanceIssueEntity issueEntity = new MaintenanceIssueEntity(issueId, roomNumber, request.getDescription().trim(), priority, now);
        issueEntity.setSource(IssueSource.DIRECT);
        issueEntity.setEngineeringImpact(EngineeringImpact.OUT_OF_ORDER);
        issueEntity.setRequiresCleaning(false);
        issueEntity.setCleanupPending(false);

        issueEntity = issueRepository.save(issueEntity);

        applyEngineeringStatus(roomNumber, roomState, now);
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
    public MutationResult executeReportInternal(CreateInternalIssueRequest request, String idempotencyKey) {
        Instant now = CanonicalTime.now();
        String roomNumber = request.getRoomNumber().trim();
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        IssueSource source = IssueSource.valueOf(request.getSource().toUpperCase(Locale.ROOT));

        EngineeringImpact engineeringImpact = switch (priority) {
            case CRITICAL -> EngineeringImpact.OUT_OF_ORDER;
            case HIGH -> EngineeringImpact.OUT_OF_SERVICE;
            case NORMAL, LOW -> EngineeringImpact.OPERATIONAL;
        };

        String fingerprint = computeFingerprint(source, roomNumber, request.getDescription(), priority, engineeringImpact);

        // Pre-check idempotency key in current transaction
        Optional<MaintenanceIssueEntity> existingOpt = issueRepository.findBySourceAndIdempotencyKey(source, idempotencyKey);
        if (existingOpt.isPresent()) {
            MaintenanceIssueEntity existing = existingOpt.get();
            if (fingerprint.equals(existing.getRequestFingerprint())) {
                log.info("Idempotent replay detected for internal issue source {} key {}", source, idempotencyKey);
                return new MutationResult(MaintenanceIssue.fromEntity(existing));
            } else {
                throw new ConflictException("IDEMPOTENCY_CONFLICT", "Conflicting payload for idempotency key: " + idempotencyKey);
            }
        }

        // Global lock order: room_states -> maintenance_issues -> technicians
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        UUID issueId = UUID.randomUUID();
        MaintenanceIssueEntity issueEntity = new MaintenanceIssueEntity(issueId, roomNumber, request.getDescription().trim(), priority, now);
        issueEntity.setSource(source);
        issueEntity.setSourceReferenceId(request.getSourceReferenceId());
        issueEntity.setIdempotencyKey(idempotencyKey);
        issueEntity.setRequestFingerprint(fingerprint);
        issueEntity.setEngineeringImpact(engineeringImpact);
        issueEntity.setRequiresCleaning(false);
        issueEntity.setCleanupPending(false);

        issueEntity = issueRepository.save(issueEntity);

        applyEngineeringStatus(roomNumber, roomState, now);
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
            log.info("Assigned technician {} to newly reported internal issue {} for room {}", tech.getName(), issueEntity.getId(), roomNumber);
        }

        return new MutationResult(MaintenanceIssue.fromEntity(issueEntity));
    }

    @Transactional
    public ProcessNextResult executeProcessNext() {
        Instant now = CanonicalTime.now();

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
    public MutationResult executeResolve(UUID issueId, ResolveIssueRequest request) {
        Instant now = CanonicalTime.now();
        boolean requiresCleaning = request != null && request.isRequiresCleaning();

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
        issue.setRequiresCleaning(requiresCleaning);
        if (requiresCleaning) {
            issue.setCleanupPending(true);
            issue.setCleanupAcknowledgedAt(null);
        } else {
            issue.setCleanupPending(false);
            issue.setCleanupAcknowledgedAt(null);
        }
        issueRepository.save(issue);

        // 4. Release technician if assigned
        String assignedTech = issue.getAssignedTechnician();
        if (assignedTech != null) {
            Long nextSeq = technicianRepository.getNextAvailabilitySequence();
            technicianRepository.releaseTechnician(assignedTech, nextSeq, now);
            log.info("Released technician {} with sequence {}", assignedTech, nextSeq);
        }

        enqueueIssueUpdated(issue.getId().toString(), roomNumber, issue.getPriority().name(), IssueStatus.RESOLVED.name(), assignedTech, now);

        if (requiresCleaning) {
            enqueueWorkCompleted(issue.getId(), roomNumber, true, issue.getResolvedAt());
        }

        // 5. Update room engineering status according to canonical derivation
        applyEngineeringStatus(roomNumber, roomState, now);

        return new MutationResult(MaintenanceIssue.fromEntity(issue));
    }

    @Transactional
    public MutationResult executeCancel(UUID issueId) {
        Instant now = CanonicalTime.now();

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
        issue.setCleanupPending(false);
        issueRepository.save(issue);

        // 4. Release technician if assigned
        String assignedTech = issue.getAssignedTechnician();
        if (assignedTech != null) {
            Long nextSeq = technicianRepository.getNextAvailabilitySequence();
            technicianRepository.releaseTechnician(assignedTech, nextSeq, now);
            log.info("Released technician {} with sequence {}", assignedTech, nextSeq);
        }

        enqueueIssueUpdated(issue.getId().toString(), roomNumber, issue.getPriority().name(), IssueStatus.CANCELLED.name(), assignedTech, now);

        // 5. Update room engineering status
        applyEngineeringStatus(roomNumber, roomState, now);

        return new MutationResult(MaintenanceIssue.fromEntity(issue));
    }

    @Transactional
    public void processCleanupCompletedWithInbox(EventEnvelope<?> envelope, com.hotelos.common.event.payload.MaintenanceCleanupCompletedPayload payload) {
        MaintenanceInboxService.InboxResult inboxResult = inboxService.registerEvent(envelope);
        if (inboxResult == MaintenanceInboxService.InboxResult.DUPLICATE) {
            log.info("Durable inbox duplicate for eventId {}; skipping business mutation", envelope.eventId());
            return;
        }
        if (inboxResult == MaintenanceInboxService.InboxResult.CONFLICT) {
            log.error("DATA INCONSISTENCY: Conflicting payload for eventId {}; skipping business mutation", envelope.eventId());
            return;
        }

        executeProcessCleanupCompleted(envelope, payload.issueId(), payload.roomNumber(), payload.cleanedAt());
    }

    @Transactional
    public void executeProcessCleanupCompleted(EventEnvelope<?> envelope, UUID issueId, String roomNumber, Instant cleanedAt) {
        Instant now = CanonicalTime.now();
        Instant normCleanedAt = CanonicalTime.normalize(cleanedAt);

        // Global lock order: room_states -> maintenance_issues
        roomStateRepository.insertIfAbsent(roomNumber, now);
        RoomStateEntity roomState = roomStateRepository.lockRoomState(roomNumber)
                .orElseThrow(() -> new IllegalStateException("Room state not found for room " + roomNumber));

        Optional<MaintenanceIssueEntity> issueOpt = issueRepository.lockIssue(issueId);
        if (issueOpt.isEmpty()) {
            log.error("CRITICAL_INTEGRITY_MISMATCH: Issue {} does not exist", issueId);
            return;
        }

        MaintenanceIssueEntity issue = issueOpt.get();
        if (!roomNumber.trim().equals(issue.getRoomNumber().trim())) {
            log.error("CRITICAL_INTEGRITY_MISMATCH: Payload room {} does not match issue room {}", roomNumber, issue.getRoomNumber());
            return;
        }

        if (!issue.isRequiresCleaning()) {
            log.error("CRITICAL_INTEGRITY_MISMATCH: Issue {} does not require cleaning", issueId);
            return;
        }

        Instant normResolvedAt = CanonicalTime.normalize(issue.getResolvedAt());
        if (normResolvedAt != null && normCleanedAt.isBefore(normResolvedAt)) {
            log.error("CRITICAL_INTEGRITY_MISMATCH: cleanedAt {} is before resolvedAt {}", normCleanedAt, normResolvedAt);
            return;
        }

        if (issue.isCleanupPending()) {
            issue.setCleanupPending(false);
            issue.setCleanupAcknowledgedAt(normCleanedAt);
            issueRepository.save(issue);
            applyEngineeringStatus(roomNumber, roomState, now);
            log.info("Cleanup acknowledged for issue {} in room {}", issueId, roomNumber);
        } else {
            Instant normAckedAt = CanonicalTime.normalize(issue.getCleanupAcknowledgedAt());
            if (normAckedAt != null && normAckedAt.equals(normCleanedAt)) {
                log.info("Logical duplicate cleanup ACK for issue {} in room {}", issueId, roomNumber);
            } else {
                log.error("CRITICAL_INTEGRITY_MISMATCH: Conflicting cleanedAt for already acknowledged issue {}. Expected {}, got {}",
                        issueId, normAckedAt, normCleanedAt);
            }
        }
    }

    public EngineeringStatus deriveEngineeringStatus(String roomNumber) {
        List<MaintenanceIssueEntity> blocking = issueRepository.findBlockingIssuesForRoom(roomNumber);
        if (blocking.isEmpty()) {
            return EngineeringStatus.OPERATIONAL;
        }
        if (blocking.stream().anyMatch(i -> i.getEngineeringImpact() == EngineeringImpact.OUT_OF_ORDER)) {
            return EngineeringStatus.OUT_OF_ORDER;
        }
        if (blocking.stream().anyMatch(i -> i.getEngineeringImpact() == EngineeringImpact.OUT_OF_SERVICE)) {
            return EngineeringStatus.OUT_OF_SERVICE;
        }
        if (blocking.stream().anyMatch(MaintenanceIssueEntity::isCleanupPending)) {
            return EngineeringStatus.OUT_OF_SERVICE;
        }
        return EngineeringStatus.OPERATIONAL;
    }

    public void applyEngineeringStatus(String roomNumber, RoomStateEntity roomState, Instant now) {
        EngineeringStatus oldStatus = roomState.getEngineeringStatus();
        EngineeringStatus targetStatus = deriveEngineeringStatus(roomNumber);
        if (targetStatus != oldStatus) {
            roomState.setEngineeringStatus(targetStatus);
            roomState.setStatusChangedAt(now);
            roomState.setUpdatedAt(now);
            roomStateRepository.save(roomState);
            enqueueRoomEngineeringChanged(roomNumber, targetStatus, now);
            log.info("Room {} transitioned from {} to {}", roomNumber, oldStatus, targetStatus);
        }
    }

    @Transactional
    public void executeDevReset() {
        outboxRepository.deleteAllInBatch();
        issueRepository.deleteAll();
        roomStateRepository.deleteAll();
        Instant now = CanonicalTime.now();
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

    private void enqueueWorkCompleted(UUID issueId, String roomNumber, boolean requiresCleaning, Instant completedAt) {
        MaintenanceWorkCompletedPayload payload = new MaintenanceWorkCompletedPayload(
                issueId,
                roomNumber,
                requiresCleaning,
                CanonicalTime.normalize(completedAt)
        );
        EventEnvelope<MaintenanceWorkCompletedPayload> envelope = EventEnvelope.create(
                EventTypes.MAINTENANCE_WORK_COMPLETED,
                "maintenance-service",
                roomNumber,
                issueId.toString(),
                payload
        );
        outboxService.enqueue(envelope, MessagingConstants.HOTEL_EXCHANGE, RoutingKeys.MAINTENANCE_WORK_COMPLETED);
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
