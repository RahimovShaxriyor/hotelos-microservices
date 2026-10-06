package com.hotelos.dashboard.model;

public enum ProjectionOutcome {
    APPLIED,
    FEED_ONLY,
    STALE,
    LOGICAL_DUPLICATE,
    INTEGRITY_CONFLICT,
    TRANSPORT_DUPLICATE,
    EVENT_ID_REUSE_ANOMALY
}
