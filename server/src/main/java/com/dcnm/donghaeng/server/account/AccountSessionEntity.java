package com.dcnm.donghaeng.server.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * DB 표 account_sessions: 발급한 계정 토큰.
 * 여행방 토큰(access_sessions)과 표를 나눠서, 계정 토큰으로 여행방을, 여행방 토큰으로 계정을 건드릴 수 없게 한다.
 */
@Entity
@Table(name = "account_sessions", indexes = @Index(name = "idx_account_sessions_account", columnList = "account_id"))
public class AccountSessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 지금은 쓰지 않음. 나중에 "다른 기기에서 로그아웃" 같은 기능을 넣을 자리. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AccountSessionEntity() {}

    public AccountSessionEntity(String id, String token, String accountId, Instant createdAt) {
        this.id = id;
        this.token = token;
        this.accountId = accountId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getToken() { return token; }
    public String getAccountId() { return accountId; }
}
