package com.dcnm.donghaeng.server.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final TripSocketHandler handler;

	public WebSocketConfig(TripSocketHandler handler) {
		this.handler = handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// TODO(④): 웹 배포 주소가 정해지면 허용 origin 좁히기
		registry.addHandler(handler, "/ws/trips/*").setAllowedOriginPatterns("*");
	}
}
