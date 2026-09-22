package com.hotelos.reception.event;

public record CheckOutCommittedEvent(String roomNumber, String correlationId) {}
