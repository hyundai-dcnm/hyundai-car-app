package com.dcnm.donghaeng.server.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** DB 표 access_sessions: 발급한 접근 토큰. 서버 재시작 시 재로그인 필요가 없도록 DB에 둔다. */
@Entity
@Table(name = "access_sessions", indexes = @Index(name = "idx_sessions_trip", columnList = "trip_id"))
public class SessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    /** 차량이면 멤버 id, 웹 참여자면 비어 있음. */
    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "display_name", nullable = false, length = 20)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TripPrincipal.Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected SessionEntity() {}

    public SessionEntity(String id, String token, String tripId, String memberId, String displayName,
                         TripPrincipal.Role role, Instant createdAt) {
        this.id = id;
        this.token = token;
        this.tripId = tripId;
        this.memberId = memberId;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getToken() { return token; }
    public String getTripId() { return tripId; }
    public String getMemberId() { return memberId; }
    public String getDisplayName() { return displayName; }
    public TripPrincipal.Role getRole() { return role; }

    public void revoke(Instant now) { this.revokedAt = now; }
}