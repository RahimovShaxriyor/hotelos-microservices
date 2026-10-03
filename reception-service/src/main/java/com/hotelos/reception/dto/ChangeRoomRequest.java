package com.hotelos.reception.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoomRequest(
        @NotBlank(message = "targetRoomNumber is required")
        String targetRoomNumber,
        String reason
) {
}
