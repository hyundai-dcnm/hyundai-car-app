package com.dcnm.donghaeng.server.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "trips")
public class TripEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 6)
    private String inviteCode;

    @Column(name = "host_member_id", length = 36)
    private String hostMemberId;

    /** 모든 차량이 수락해 확정된 목적지. 확정 전에는 비어 있다. */
    @Column(name = "destination_name", length = 100)
    private String destinationName;

    @Column(name = "destination_lat")
    private Double destinationLat;

    @Column(name = "destination_lng")
    private Double destinationLng;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 마지막 차량이 나가면 작성되며 닫힌 방의 초대 코드로는 들어올 수 없다. */
    @Column(name = "closed_at")
    private Instant closedAt;

    protected TripEntity() {}

    public TripEntity(String id, String name, String inviteCode, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public String getHostMemberId() { return hostMemberId; }
    public String getDestinationName() { return destinationName; }
    public Double getDestinationLat() { return destinationLat; }
    public Double getDestinationLng() { return destinationLng; }
    public boolean isClosed() { return closedAt != null; }

    public void setHostMemberId(String hostMemberId) { this.hostMemberId = hostMemberId; }

    public void confirmDestination(String name, double lat, double lng) {
        this.destinationName = name;
        this.destinationLat = lat;
        this.destinationLng = lng;
    }

    public void close(Instant now) { this.closedAt = now; }
}