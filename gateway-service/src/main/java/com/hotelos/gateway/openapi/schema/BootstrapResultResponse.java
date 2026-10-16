package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "BootstrapResultDto", description = "Summary of dashboard database initialization from authoritative services")
public record BootstrapResultResponse(
        @Schema(description = "Bootstrap execution status", example = "BOOTSTRAP_COMPLETE")
        String status,
        @Schema(description = "Sources synchronized map", example = "{\"rooms\":12,\"stays\":4}")
        Map<String, Object> sources,
        @Schema(description = "Count of hotel rooms projected into persistent read-model", example = "12")
        int totalRoomsProjected
) {}
