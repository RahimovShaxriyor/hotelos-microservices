package com.hotelos.roomservice.dto;

import java.util.UUID;

public record ActiveStayResolutionResponse(
        UUID stayId,
        String roomNumber,
        String currency
) {
}
