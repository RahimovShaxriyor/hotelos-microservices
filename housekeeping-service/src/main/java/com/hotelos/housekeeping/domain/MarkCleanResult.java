package com.hotelos.housekeeping.domain;

public record MarkCleanResult(
        MarkCleanStatus status,
        CleaningTask task
) {
}
