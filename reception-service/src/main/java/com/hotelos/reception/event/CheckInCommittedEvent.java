package com.hotelos.reception.event;

import com.hotelos.reception.domain.OccupancyStatus;

public record CheckInCommittedEvent(String roomNumber, OccupancyStatus occupancyStatus) {}
