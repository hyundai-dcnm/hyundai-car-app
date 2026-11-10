package com.dcnm.donghaeng.server.ws;

import com.dcnm.donghaeng.server.trip.TripBroadcaster;
import com.dcnm.donghaeng.server.trip.TripEvent;
import com.dcnm.donghaeng.server.trip.TripService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * /ws/trips/{tripId}?token=... 접속하자마자 snapshot 을 보낸다.
 * 앱에서 받는 것은 location 이벤트뿐이고, 나머지 변경은 REST 로 들어온다.
 */
@Component
public class TripSocketHandler extends TextWebSocketHandler implements TripBroadcaster {

	private static final String SESSION = "session";

	private final TripService trips;
	private final ObjectMapper json;
	private final Map<String, Set<WebSocketSession>> byTrip = new ConcurrentHashMap<>();

	public TripSocketHandler(@org.springframework.context.annotation.Lazy TripService trips, ObjectMapper json) {
		this.trips = trips;
		this.json = json;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession raw) throws Exception {
		var uri = UriComponentsBuilder.fromUri(raw.getUri()).build();
		String tripId = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
		String token = uri.getQueryParams().getFirst("token");
		var session = trips.session(token).filter(s -> s.tripId().equals(tripId));
		if (session.isEmpty()) {
			raw.close(CloseStatus.POLICY_VIOLATION.withReason("unauthorized"));
			return;
		}
		var ws = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 256 * 1024);
		raw.getAttributes().put(SESSION, session.get());
		byTrip.computeIfAbsent(tripId, id -> ConcurrentHashMap.newKeySet()).add(ws);
		ws.sendMessage(new TextMessage(json.writeValueAsString(trips.snapshot(tripId))));
	}

	@Override
	protected void handleTextMessage(WebSocketSession ws, TextMessage message) {
		var session = (TripService.Session) ws.getAttributes().get(SESSION);
		if (session == null) return;
		TripEvent event;
		try {
			event = json.readValue(message.getPayload(), TripEvent.class);
		} catch (Exception e) {
			return;
		}
		if (event instanceof TripEvent.LocationUpdated updated) {
			trips.updateLocation(session, updated.location());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
		byTrip.values().forEach(set -> set.removeIf(s -> s.getId().equals(ws.getId())));
	}

	@Override
	public void send(String tripId, TripEvent event) {
		var targets = byTrip.get(tripId);
		if (targets == null) return;
		var text = new TextMessage(json.writeValueAsString(event));
		for (var ws : targets) {
			try {
				ws.sendMessage(text);
			} catch (IOException | IllegalStateException ignored) {
				// 끊긴 세션은 afterConnectionClosed 에서 빠진다.
			}
		}
	}
}
