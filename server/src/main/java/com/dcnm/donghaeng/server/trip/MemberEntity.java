package com.dcnm.donghaeng.server.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** DB 표 trip_members: 입장한 차량. 퇴장 시 지우지 않고 left_at 만 채운다(앨범 및 기록용). */
@Entity
@Table(name = "trip_members", indexes = @Index(name = "idx_members_trip", columnList = "trip_id"))
public class MemberEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "car_label", nullable = false, length = 20)
    private String carLabel;

    @Column(name = "sharing_location", nullable = false)
    private boolean sharingLocation = true;

    /** 계정으로 들어온 차량이면 계정 id. 계정 없이 들어오면 비어 있다. 친구 초대(다음 단계)에서 쓴다. */
    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected MemberEntity() {}

    /** accountId 는 계정 없이 들어오면 null. */
    public MemberEntity(String id, String tripId, String nickname, String carLabel, String accountId, Instant joinedAt) {
        this.id = id;
        this.tripId = tripId;
        this.nickname = nickname;
        this.carLabel = carLabel;
        this.accountId = accountId;
        this.joinedAt = joinedAt;
    }

    public String getId() { return id; }
    public String getTripId() { return tripId; }
    public String getNickname() { return nickname; }
    public String getCarLabel() { return carLabel; }
    public String getAccountId() { return accountId; }
    public boolean isSharingLocation() { return sharingLocation; }

    public void setSharingLocation(boolean sharingLocation) { this.sharingLocation = sharingLocation; }

    public void leave(Instant now) { this.leftAt = now; }
}