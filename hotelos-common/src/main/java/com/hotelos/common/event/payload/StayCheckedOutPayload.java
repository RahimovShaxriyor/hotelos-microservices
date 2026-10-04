package com.hotelos.common.event.payload;

import java.time.Instant;
import java.util.UUID;

public record StayCheckedOutPayload(
        UUID stayId,
        UUID folioId,
        Instant checkedOutAt,
        UUID freezeId
) {
}
