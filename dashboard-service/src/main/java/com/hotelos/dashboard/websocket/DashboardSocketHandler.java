package com.hotelos.dashboard.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DashboardSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final String token;

    public DashboardSocketHandler(@Value("${hotelos.dashboard-token}") String token) {
        this.token = token;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri() == null ? "" : session.getUri().getQuery();
        if (query == null || !query.contains("token=" + token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid dashboard token"));
            return;
        }
        sessions.add(session);
        session.sendMessage(new TextMessage("{\"event\":\"dashboard.connected\",\"message\":\"Connected to HotelOS live dashboard\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcast(String json) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException ignored) {
                    sessions.remove(session);
                }
            }
        }
    }
}
