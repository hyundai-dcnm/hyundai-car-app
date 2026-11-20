package com.dcnm.donghaeng.server.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** DB 표 accounts: 기기 계정. 비밀번호 없이 닉네임·차량 이름과 친구 코드만 가진다. */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "car_label", nullable = false, length = 20)
    private String carLabel;

    /** 친구 요청할 때 알려 주는 6자리. 초대 코드와 같은 글자 규칙. 바뀌지 않는다. */
    @Column(name = "friend_code", nullable = false, unique = true, length = 6)
    private String friendCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountEntity() {}

    public AccountEntity(String id, String nickname, String carLabel, String friendCode, Instant createdAt) {
        this.id = id;
        this.nickname = nickname;
        this.carLabel = carLabel;
        this.friendCode = friendCode;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public String getCarLabel() { return carLabel; }
    public String getFriendCode() { return friendCode; }

    public void rename(String nickname, String carLabel, Instant now) {
        this.nickname = nickname;
        this.carLabel = carLabel;
        this.updatedAt = now;
    }
}
