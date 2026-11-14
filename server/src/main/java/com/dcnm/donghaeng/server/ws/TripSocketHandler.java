package com.dcnm.donghaeng.server.ws;

import com.dcnm.donghaeng.server.auth.AccessTokenService;
import com.dcnm.donghaeng.server.auth.TripPrincipal;
import com.dcnm.donghaeng.server.trip.TripEvent;
import com.dcnm.donghaeng.server.trip.TripEventMessage;
import com.dcnm.donghaeng.server.trip.TripModels.LatLng;
import com.dcnm.donghaeng.server.trip.TripService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * /ws/trips/{tripId}?token=... 실시간 연결.
 * 1) 접속하자마자 snapshot(방 전체 상태 + 일행 위치)을 보낸다. 재접속해도 똑같이 보내므로 앱은 끊긴 동안을 따로 맞추지 않는다.
 * 2) 앱이 보내는 것은 location 하나이며, 나머지 변경은 REST 로 들어오고 서비스가 낸 이벤트를 여기서 방 전원에게 보낸다.
 */
@Component
public class TripSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TripSocketHandler.class);
    private static final String PRINCIPAL = "principal";

    private final TripService trips;
    private final AccessTokenService tokens;
    private final ObjectMapper json;
    private final Map<String, Set<WebSocketSession>> byTrip = new ConcurrentHashMap<>();

    public TripSocketHandler(TripService trips, AccessTokenService tokens, ObjectMapper json) {
        this.trips = trips;
        this.tokens = tokens;
        this.json = json;
    }

    /** 메시지 형태: {"type":"location","location":{"location":{"lat":..,"lng":..},"headingDeg":..,"speedKph":..}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Incoming(String type, LocationIn location) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LocationIn(LatLng location, Double headingDeg, Double speedKph) {}

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) throws Exception {
        var uri = UriComponentsBuilder.fromUri(raw.getUri()).build();
        List<String> segments = uri.getPathSegments();
        String tripId = segments.get(segments.size() - 1);
        String token = uri.getQueryParams().getFirst("token");
        Optional<TripPrincipal> principal = tokens.resolve(token).filter(p -> p.tripId().equals(tripId));
        if (principal.isEmpty()) {
            raw.close(CloseStatus.POLICY_VIOLATION.withReason("unauthorized"));
            return;
        }
        TripEvent.Snapshot snapshot;
        try {
            snapshot = trips.snapshot(tripId);
        } catch (ResponseStatusException e) {
            raw.close(CloseStatus.POLICY_VIOLATION.withReason("trip closed"));
            return;
        }
        raw.getAttributes().put(PRINCIPAL, principal.get());
        WebSocketSession ws = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 256 * 1024);
        byTrip.computeIfAbsent(tripId, id -> ConcurrentHashMap.newKeySet()).add(ws);
        ws.sendMessage(new TextMessage(json.writeValueAsString(snapshot)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (!(session.getAttributes().get(PRINCIPAL) instanceof TripPrincipal me)) return;
        try {
            Incoming in = json.readValue(message.getPayload(), Incoming.class);
            if ("location".equals(in.type()) && in.location() != null) {
                LocationIn l = in.location();
                trips.relayLocation(me, l.location(), l.headingDeg(), l.speedKph());
            }
        } catch (RuntimeException e) {
            // 형식이 틀린 메시지는 버리며 연결은 끊지 않는다.
            log.debug("ignored message from {}: {}", me.memberId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        byTrip.values().forEach(set -> set.removeIf(s -> s.getId().equals(session.getId())));
    }

    /** 서비스가 낸 이벤트를 DB 저장이 끝난 뒤(AFTER_COMMIT) 방 접속자 전원에게 보낸다. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onTripEvent(TripEventMessage message) {
        Set<WebSocketSession> targets = byTrip.get(message.tripId());
        if (targets == null || targets.isEmpty()) return;
        TextMessage text = new TextMessage(json.writeValueAsString(message.event()));
        for (WebSocketSession ws : targets) {
            send(ws, text);
        }
        if (message.event() instanceof TripEvent.MemberLeft left) {
            closeWhere(message.tripId(), p -> left.memberId().equals(p.memberId()), "left");
        }
        if (message.event() instanceof TripEvent.TripClosed) {
            closeWhere(message.tripId(), p -> true, "trip closed");
            byTrip.remove(message.tripId());
        }
    }

    private void send(WebSocketSession ws, TextMessage text) {
        try {
            ws.sendMessage(text);
        } catch (IOException | IllegalStateException e) {
            // 끊긴 연결은 afterConnectionClosed 에서 빠진다.
            log.debug("send failed: {}", e.getMessage());
        }
    }

    private void closeWhere(String tripId, java.util.function.Predicate<TripPrincipal> match, String reason) {
        Set<WebSocketSession> targets = byTrip.get(tripId);
        if (targets == null) return;
        for (WebSocketSession ws : targets) {
            if (ws.getAttributes().get(PRINCIPAL) instanceof TripPrincipal p && match.test(p)) {
                try {
                    ws.close(CloseStatus.NORMAL.withReason(reason));
                } catch (IOException ignored) {
                    // 이미 끊김
                }
            }
        }
    }
}