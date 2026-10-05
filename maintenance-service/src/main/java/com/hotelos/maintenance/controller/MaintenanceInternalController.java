package com.hotelos.maintenance.controller;

import com.hotelos.maintenance.domain.MaintenanceIssue;
import com.hotelos.maintenance.dto.CreateInternalIssueRequest;
import com.hotelos.maintenance.service.MaintenanceWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance/internal")
public class MaintenanceInternalController {

    private final MaintenanceWorkflowService maintenanceWorkflowService;

    public MaintenanceInternalController(MaintenanceWorkflowService maintenanceWorkflowService) {
        this.maintenanceWorkflowService = maintenanceWorkflowService;
    }

    @PostMapping("/issues")
    public MaintenanceIssue reportInternal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateInternalIssueRequest request
    ) {
        return maintenanceWorkflowService.reportInternal(request, idempotencyKey);
    }
}
