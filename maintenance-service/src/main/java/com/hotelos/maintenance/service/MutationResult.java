package com.hotelos.maintenance.service;

import com.hotelos.maintenance.domain.MaintenanceIssue;

public record MutationResult(
        MaintenanceIssue issue
) {
}
