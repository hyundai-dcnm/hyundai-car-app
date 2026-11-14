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

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected MemberEntity() {}

    public MemberEntity(String id, String tripId, String nickname, String carLabel, Instant joinedAt) {
        this.id = id;
        this.tripId = tripId;
        this.nickname = nickname;
        this.carLabel = carLabel;
        this.joinedAt = joinedAt;
    }

    public String getId() { return id; }
    public String getTripId() { return tripId; }
    public String getNickname() { return nickname; }
    public String getCarLabel() { return carLabel; }
    public boolean isSharingLocation() { return sharingLocation; }

    public void setSharingLocation(boolean sharingLocation) { this.sharingLocation = sharingLocation; }

    public void leave(Instant now) { this.leftAt = now; }
}