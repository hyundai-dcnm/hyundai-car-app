package com.dcnm.donghaeng.server.location;

import com.dcnm.donghaeng.server.trip.TripModels.MemberLocation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 차량별 마지막 위치. DB 에 저장하지 않고 메모리에만 둔다.
 * 새로 접속한 기기가 snapshot 으로 일행 위치를 바로 보게 하는 용도.
 */
@Component
public class LocationRelay {

    /** 이보다 오래된 위치는 snapshot 에 넣지 않는다. */
    private static final long STALE_MILLIS = 120_000;
    /** 한 차량이 이보다 자주 보내면 버린다(서버/DB 부담 방지). */
    private static final long MIN_INTERVAL_MILLIS = 500;

    private final Map<String, Map<String, MemberLocation>> byTrip = new ConcurrentHashMap<>();

    /** 저장했으면 true, 너무 자주 와서 버렸으면 false. */
    public boolean put(String tripId, MemberLocation location) {
        Map<String, MemberLocation> trip = byTrip.computeIfAbsent(tripId, id -> new ConcurrentHashMap<>());
        MemberLocation previous = trip.get(location.memberId());
        if (previous != null && location.updatedAtMillis() - previous.updatedAtMillis() < MIN_INTERVAL_MILLIS) {
            return false;
        }
        trip.put(location.memberId(), location);
        return true;
    }

    public void remove(String tripId, String memberId) {
        Map<String, MemberLocation> trip = byTrip.get(tripId);
        if (trip != null) trip.remove(memberId);
    }

    public void removeTrip(String tripId) {
        byTrip.remove(tripId);
    }

    public List<MemberLocation> list(String tripId) {
        long now = System.currentTimeMillis();
        return byTrip.getOrDefault(tripId, Map.of()).values().stream()
                .filter(l -> now - l.updatedAtMillis() < STALE_MILLIS)
                .toList();
    }
}