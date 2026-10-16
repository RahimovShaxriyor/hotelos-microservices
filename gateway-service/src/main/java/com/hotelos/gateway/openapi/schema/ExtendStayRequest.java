package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "ExtendStayRequest", description = "Payload to extend the departure date of an active stay")
public record ExtendStayRequest(
        @Schema(description = "New departure date strictly later than current departure date", example = "2026-10-14", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate newDepartureDate
) {}
