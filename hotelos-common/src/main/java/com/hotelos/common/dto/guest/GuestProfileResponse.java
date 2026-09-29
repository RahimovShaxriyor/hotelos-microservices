package com.hotelos.common.dto.guest;

import java.time.Instant;
import java.util.UUID;

public record GuestProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean vipStatus,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
