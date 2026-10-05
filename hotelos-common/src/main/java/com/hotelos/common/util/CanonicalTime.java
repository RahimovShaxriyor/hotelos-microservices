package com.hotelos.common.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class CanonicalTime {
    private CanonicalTime() {}

    public static Instant now() {
        return normalize(Instant.now());
    }

    public static Instant normalize(Instant instant) {
        return instant != null ? instant.truncatedTo(ChronoUnit.MICROS) : null;
    }
}
