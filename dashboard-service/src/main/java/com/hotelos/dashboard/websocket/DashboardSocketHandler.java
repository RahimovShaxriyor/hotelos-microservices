package com.hotelos.dashboard.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DashboardSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public DashboardSocketHandler() {
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        String principalName = session.getPrincipal() != null ? session.getPrincipal().getName() : "anonymous";
        log.info("Dashboard WebSocket session established for principal: {}", principalName);
        session.sendMessage(new TextMessage("{\"event\":\"dashboard.connected\",\"message\":\"Connected to HotelOS live dashboard\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Dashboard WebSocket session closed: id={}, status={}", session.getId(), status);
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
