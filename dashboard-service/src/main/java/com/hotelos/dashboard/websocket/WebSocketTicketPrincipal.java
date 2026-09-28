package com.hotelos.dashboard.websocket;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WebSocketTicketPrincipal implements Principal {

    private final String name;
    private final String subject;
    private final String username;
    private final List<String> roles;
    private final UUID jti;

    public WebSocketTicketPrincipal(String subject, String username, List<String> roles, UUID jti) {
        this.subject = subject;
        this.username = username;
        this.roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
        this.jti = jti;
        this.name = (username != null && !username.isBlank()) ? username : subject;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public UUID getJti() {
        return jti;
    }
}
