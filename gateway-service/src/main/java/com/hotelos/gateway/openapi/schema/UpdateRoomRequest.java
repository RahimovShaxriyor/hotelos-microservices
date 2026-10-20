package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "UpdateRoomRequest", description = "Payload to update room floor, proximity, or nightly rate")
public record UpdateRoomRequest(
        @Schema(description = "Updated floor level", example = "3")
        Integer floor,
        @Schema(description = "Updated architectural proximity", example = "VIEW", allowableValues = {"LIFT", "STAIRS", "QUIET", "VIEW"})
        String proximity,
        @Schema(description = "Updated nightly rate", example = "275000.00")
        BigDecimal nightlyRate
) {}
