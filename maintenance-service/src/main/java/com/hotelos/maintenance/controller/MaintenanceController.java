package com.hotelos.maintenance.controller;

import com.hotelos.maintenance.domain.MaintenanceIssue;
import com.hotelos.maintenance.dto.CreateIssueRequest;
import com.hotelos.maintenance.service.MaintenanceWorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private final MaintenanceWorkflowService maintenanceWorkflowService;

    public MaintenanceController(MaintenanceWorkflowService maintenanceWorkflowService) {
        this.maintenanceWorkflowService = maintenanceWorkflowService;
    }

    @PostMapping("/issues")
    public MaintenanceIssue report(@RequestBody CreateIssueRequest request) {
        return maintenanceWorkflowService.report(request);
    }

    @GetMapping("/issues")
    public List<MaintenanceIssue> issues() {
        return maintenanceWorkflowService.getIssues();
    }

    @GetMapping("/issues/{issueId}")
    public MaintenanceIssue issue(@PathVariable String issueId) {
        return maintenanceWorkflowService.getIssue(issueId);
    }

    @GetMapping("/queue")
    public List<MaintenanceIssue> queue() {
        return maintenanceWorkflowService.getPriorityQueueSnapshot();
    }

    @PostMapping("/queue/process-next")
    public List<MaintenanceIssue> processNext() {
        return maintenanceWorkflowService.processNext();
    }

    @PatchMapping("/issues/{issueId}/resolve")
    public MaintenanceIssue resolvePatch(@PathVariable String issueId) {
        return maintenanceWorkflowService.resolve(issueId);
    }

    @PostMapping("/issues/{issueId}/resolve")
    public MaintenanceIssue resolvePost(@PathVariable String issueId) {
        return maintenanceWorkflowService.resolve(issueId);
    }

    @PatchMapping("/issues/{issueId}/cancel")
    public MaintenanceIssue cancel(@PathVariable String issueId) {
        return maintenanceWorkflowService.cancel(issueId);
    }

    @GetMapping("/technicians")
    public List<String> technicians() {
        return maintenanceWorkflowService.getTechnicians();
    }

    @PostMapping("/dev/reset")
    public Map<String, Object> reset() {
        return maintenanceWorkflowService.reset();
    }

    @PostMapping("/dev/seed")
    public Map<String, Object> seed() {
        return maintenanceWorkflowService.reset();
    }
}
