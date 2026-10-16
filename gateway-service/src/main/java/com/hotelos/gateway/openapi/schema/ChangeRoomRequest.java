package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ChangeRoomRequest", description = "Payload to transfer an active stay into another clean ready room")
public record ChangeRoomRequest(
        @Schema(description = "Target physical room number", example = "301", requiredMode = Schema.RequiredMode.REQUIRED)
        String targetRoomNumber,
        @Schema(description = "Operational reason for transfer", example = "Guest requested upper floor")
        String reason
) {}
