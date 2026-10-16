package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "DashboardSnapshotResponse", description = "Consolidated full hotel operational snapshot")
public record DashboardSnapshotResponse(
        @Schema(description = "All hotel rooms and real-time operational states")
        List<RoomResponse> rooms,
        @Schema(description = "Active guest stays")
        List<GuestStayResponse> guests,
        @Schema(description = "Active and recent room service orders")
        List<RoomServiceOrderResponse> roomServiceOrders,
        @Schema(description = "Active and recent maintenance issues")
        List<MaintenanceIssueResponse> maintenanceIssues,
        @Schema(description = "Current housekeeping cleaning tasks queue")
        List<CleaningTaskResponse> housekeepingQueue,
        @Schema(description = "Recent system domain events")
        List<EventFeedResponse> events
) {}
