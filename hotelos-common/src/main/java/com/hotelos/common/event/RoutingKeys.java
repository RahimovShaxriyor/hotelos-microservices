package com.hotelos.common.event;

public final class RoutingKeys {
    private RoutingKeys() {}

    public static final String ROOM_VACATED = "room.vacated";
    public static final String ROOM_OCCUPANCY_CHANGED = "reception.room.occupancy.changed";
    public static final String ROOM_HOUSEKEEPING_CHANGED = "housekeeping.room.status.changed";
    public static final String ROOM_ENGINEERING_CHANGED = "maintenance.room.status.changed";
    public static final String ROOM_SERVICE_CHARGE = "room.service.charge";
    public static final String ROOM_SERVICE_ORDER_UPDATED = "room.service.order.updated";
    public static final String MAINTENANCE_ISSUE_UPDATED = "maintenance.issue.updated";
}
