package com.hotelos.maintenance.service;

import com.hotelos.maintenance.domain.MaintenanceIssue;

import java.util.List;

public record ProcessNextResult(
        MaintenanceIssue assignedIssue,
        List<MaintenanceIssue> queueSnapshot
) {
}
