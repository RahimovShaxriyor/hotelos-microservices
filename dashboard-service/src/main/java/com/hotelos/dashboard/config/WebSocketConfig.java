package com.hotelos.dashboard.config;

import com.hotelos.dashboard.websocket.DashboardSocketHandler;
import com.hotelos.dashboard.websocket.WebSocketHandshakeHandler;
import com.hotelos.dashboard.websocket.WebSocketOriginValidator;
import com.hotelos.dashboard.websocket.WebSocketTicketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DashboardSocketHandler dashboardSocketHandler;
    private final WebSocketTicketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketHandshakeHandler handshakeHandler;
    private final WebSocketOriginValidator originValidator;

    public WebSocketConfig(
            DashboardSocketHandler dashboardSocketHandler,
            WebSocketTicketHandshakeInterceptor handshakeInterceptor,
            WebSocketHandshakeHandler handshakeHandler,
            WebSocketOriginValidator originValidator
    ) {
        this.dashboardSocketHandler = dashboardSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.handshakeHandler = handshakeHandler;
        this.originValidator = originValidator;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = originValidator.getAllowedOrigins().toArray(new String[0]);
        registry.addHandler(dashboardSocketHandler, "/ws/dashboard")
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(origins);
    }
}
