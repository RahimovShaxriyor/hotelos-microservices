package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportDefectRequest", description = "Payload to report a physical room defect discovered during housekeeping")
public record ReportDefectRequest(
        @Schema(description = "Description of physical defect", example = "Leaking bathroom faucet", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @Schema(description = "Urgency priority", example = "HIGH", allowableValues = {"CRITICAL", "HIGH", "NORMAL", "LOW"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String priority
) {}
