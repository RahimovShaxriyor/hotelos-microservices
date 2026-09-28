package com.hotelos.dashboard.websocket;

import java.util.List;

public sealed interface TicketValidationResult {
    record Success(WebSocketTicketPrincipal principal) implements TicketValidationResult {}
    record InvalidTicket(String reason) implements TicketValidationResult {}
    record ForbiddenRole(List<String> roles) implements TicketValidationResult {}
    record ServiceUnavailable(String error) implements TicketValidationResult {}
}
