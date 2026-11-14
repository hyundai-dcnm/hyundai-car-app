package com.dcnm.donghaeng.server.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** /ws/trips/{tripId} 주소에 TripSocketHandler 를 연결. 차량 앱(OkHttp)은 Origin 을 안 보내서 항상 허용된다. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TripSocketHandler handler;
    private final String[] allowedOrigins;

    public WebSocketConfig(TripSocketHandler handler, @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/trips/*").setAllowedOriginPatterns(allowedOrigins);
    }
}