package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "CreateAdjustmentRequest", description = "Payload to post manual financial adjustment on a folio")
public record CreateAdjustmentRequest(
        @Schema(description = "Entry classification type", example = "ADJUSTMENT", allowableValues = {"CHARGE", "PAYMENT", "ADJUSTMENT"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String entryType,
        @Schema(description = "Adjustment monetary amount", example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,
        @Schema(description = "Business justification or invoice note", example = "Manager discount for noise disturbance", requiredMode = Schema.RequiredMode.REQUIRED)
        String description
) {}
