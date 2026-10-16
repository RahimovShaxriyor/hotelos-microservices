package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "CleaningTask", description = "Housekeeping queue cleaning task")
public record CleaningTaskResponse(
        @Schema(description = "Room number being cleaned", example = "204")
        String roomNumber,
        @Schema(description = "Turnover cycle tracking correlation ID", example = "7cdcb4df-c453-4fc7-9735-f4fd73f81122")
        String correlationId,
        @Schema(description = "Cleaning progress status", example = "WAITING", allowableValues = {"WAITING", "CLEANING", "CLEAN", "CANCELLED"})
        String status,
        @Schema(description = "Task creation timestamp", example = "2026-10-10T11:00:00Z")
        Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-10-10T11:00:00Z")
        Instant updatedAt,
        @Schema(description = "Assigned cleaner staff member", example = "Dilnoza")
        String assignedCleaner
) {}
