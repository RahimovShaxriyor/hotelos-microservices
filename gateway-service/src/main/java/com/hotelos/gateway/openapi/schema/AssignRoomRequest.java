package com.hotelos.gateway.openapi.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AssignRoomRequest", description = "Payload to assign a physical room to a reservation")
public record AssignRoomRequest(
        @Schema(description = "Physical room number to allocate", example = "204", requiredMode = Schema.RequiredMode.REQUIRED)
        String roomNumber
) {}
