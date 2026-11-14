package com.dcnm.donghaeng.server.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 접근 토큰 발급/확인/폐기. 토큰 하나는 여행방 하나에만 유효하다.
 */
@Service
public class AccessTokenService {

    private final SecureRandom random = new SecureRandom();
    private final SessionRepository sessions;

    public AccessTokenService(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Transactional
    public String issueForCar(String tripId, String memberId, String displayName) {
        return issue(tripId, memberId, displayName, TripPrincipal.Role.CAR);
    }

    @Transactional
    public String issueForViewer(String tripId, String displayName) {
        return issue(tripId, null, displayName, TripPrincipal.Role.WEB);
    }

    @Transactional(readOnly = true)
    public Optional<TripPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessions.findByTokenAndRevokedAtIsNull(token.trim())
                .map(s -> new TripPrincipal(s.getId(), s.getTripId(), s.getMemberId(), s.getDisplayName(), s.getRole()));
    }

    /** 퇴장한 차량의 토큰을 못 쓰게 한다. */
    @Transactional
    public void revokeMember(String tripId, String memberId) {
        Instant now = Instant.now();
        sessions.findByTripIdAndMemberIdAndRevokedAtIsNull(tripId, memberId).forEach(s -> s.revoke(now));
    }

    /** 방이 닫히면 웹 참여자 토큰을 모두 못 쓰게 한다. */
    @Transactional
    public void revokeTrip(String tripId) {
        Instant now = Instant.now();
        sessions.findByTripIdAndRevokedAtIsNull(tripId).forEach(s -> s.revoke(now));
    }

    private String issue(String tripId, String memberId, String displayName, TripPrincipal.Role role) {
        var session = new SessionEntity(UUID.randomUUID().toString(), newToken(), tripId, memberId, displayName, role,
                Instant.now());
        return sessions.save(session).getToken();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}