package com.hotelos.maintenance.service;

import com.hotelos.maintenance.domain.EngineeringImpact;
import com.hotelos.maintenance.domain.IssuePriority;
import com.hotelos.maintenance.domain.IssueSource;
import com.hotelos.maintenance.domain.MaintenanceIssue;
import com.hotelos.maintenance.dto.CreateInternalIssueRequest;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.dto.ResolveIssueRequest;
import com.hotelos.maintenance.exception.ConflictException;
import com.hotelos.maintenance.exception.HotelValidationException;
import com.hotelos.maintenance.persistence.entity.MaintenanceIssueEntity;
import com.hotelos.maintenance.persistence.entity.TechnicianEntity;
import com.hotelos.maintenance.persistence.repository.MaintenanceIssueRepository;
import com.hotelos.maintenance.persistence.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MaintenanceWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceWorkflowService.class);

    private final MaintenanceTransactionService txService;
    private final MaintenanceIssueRepository issueRepository;
    private final TechnicianRepository technicianRepository;

    public MaintenanceWorkflowService(MaintenanceTransactionService txService,
                                      MaintenanceIssueRepository issueRepository,
                                      TechnicianRepository technicianRepository) {
        this.txService = txService;
        this.issueRepository = issueRepository;
        this.technicianRepository = technicianRepository;
    }

    public MaintenanceIssue report(CreateIssueRequest request) {
        validate(request);
        MutationResult result = txService.executeReport(request);
        return result.issue();
    }

    public MaintenanceIssue reportInternal(CreateInternalIssueRequest request, String idempotencyKey) {
        validateInternal(request, idempotencyKey);
        IssuePriority priority = IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        IssueSource source = IssueSource.valueOf(request.getSource().toUpperCase(Locale.ROOT));

        EngineeringImpact engineeringImpact = switch (priority) {
            case CRITICAL -> EngineeringImpact.OUT_OF_ORDER;
            case HIGH -> EngineeringImpact.OUT_OF_SERVICE;
            case NORMAL, LOW -> EngineeringImpact.OPERATIONAL;
        };

        String fingerprint = MaintenanceTransactionService.computeFingerprint(
                source, request.getRoomNumber(), request.getDescription(), priority, engineeringImpact
        );

        try {
            MutationResult result = txService.executeReportInternal(request, idempotencyKey.trim());
            return result.issue();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Unique constraint conflict on source {} key {}. Attempting two-phase read recovery.", source, idempotencyKey);
            Optional<MaintenanceIssueEntity> existingOpt = txService.findExistingBySourceAndKey(source, idempotencyKey.trim());
            if (existingOpt.isPresent()) {
                MaintenanceIssueEntity existing = existingOpt.get();
                if (fingerprint.equals(existing.getRequestFingerprint())) {
                    log.info("Two-phase recovery succeeded for source {} key {}", source, idempotencyKey);
                    return MaintenanceIssue.fromEntity(existing);
                } else {
                    throw new ConflictException("IDEMPOTENCY_CONFLICT", "Conflicting payload for idempotency key: " + idempotencyKey);
                }
            }
            throw ex;
        }
    }

    public MaintenanceIssue getIssue(String issueIdStr) {
        UUID issueId = parseUuid(issueIdStr);
        return issueRepository.findById(issueId)
                .map(MaintenanceIssue::fromEntity)
                .orElseThrow(() -> new HotelValidationException("Unknown maintenance issue ID: " + issueIdStr));
    }

    public List<MaintenanceIssue> getIssues() {
        return issueRepository.findAllOrderedByCreatedAt().stream()
                .map(MaintenanceIssue::fromEntity)
                .toList();
    }

    public List<MaintenanceIssue> getPriorityQueueSnapshot() {
        return txService.loadQueueSnapshot();
    }

    public List<MaintenanceIssue> processNext() {
        ProcessNextResult result = txService.executeProcessNext();
        return result.queueSnapshot();
    }

    public MaintenanceIssue resolve(String issueIdStr, ResolveIssueRequest request) {
        UUID issueId = parseUuid(issueIdStr);
        // TX1: room -> target issue -> technician release
        MutationResult result = txService.executeResolve(issueId, request);

        // TX2: Decoupled processNext()
        try {
            txService.executeProcessNext();
        } catch (Exception ex) {
            log.warn("Automatic reassignment post-resolve failed (TX1 remains committed): {}", ex.getMessage());
        }

        return result.issue();
    }

    public MaintenanceIssue cancel(String issueIdStr) {
        UUID issueId = parseUuid(issueIdStr);
        // TX1: room -> target issue -> technician release
        MutationResult result = txService.executeCancel(issueId);

        // TX2: Decoupled processNext()
        try {
            txService.executeProcessNext();
        } catch (Exception ex) {
            log.warn("Automatic reassignment post-cancel failed (TX1 remains committed): {}", ex.getMessage());
        }

        return result.issue();
    }

    public List<String> getTechnicians() {
        return technicianRepository.findAllOrdered().stream()
                .map(TechnicianEntity::getName)
                .toList();
    }

    public Map<String, Object> reset() {
        txService.executeDevReset();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Maintenance issues and priority queue have been cleared");
        response.put("issues", 0);
        response.put("availableTechnicians", List.of("Tech-1", "Tech-2"));
        return response;
    }

    private UUID parseUuid(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            throw new HotelValidationException("Issue ID cannot be empty");
        }
        try {
            return UUID.fromString(idStr.trim());
        } catch (IllegalArgumentException ex) {
            throw new HotelValidationException("Invalid issue ID format: " + idStr);
        }
    }

    private void validate(CreateIssueRequest request) {
        if (request == null || request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new HotelValidationException("Issue description is required");
        }
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            throw new HotelValidationException("Priority is required");
        }
        try {
            IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new HotelValidationException("Priority must be CRITICAL, HIGH, NORMAL or LOW");
        }
    }

    private void validateInternal(CreateInternalIssueRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.trim().length() > 128) {
            throw new HotelValidationException("Idempotency-Key is required and must be between 1 and 128 characters");
        }
        if (request == null || request.getRoomNumber() == null || request.getRoomNumber().isBlank()) {
            throw new HotelValidationException("Room number is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new HotelValidationException("Issue description is required");
        }
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            throw new HotelValidationException("Priority is required");
        }
        try {
            IssuePriority.valueOf(request.getPriority().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new HotelValidationException("Priority must be CRITICAL, HIGH, NORMAL or LOW");
        }
        if (request.getSource() == null || !request.getSource().equalsIgnoreCase("HOUSEKEEPING")) {
            throw new HotelValidationException("Internal issue source must be HOUSEKEEPING");
        }
    }
}
