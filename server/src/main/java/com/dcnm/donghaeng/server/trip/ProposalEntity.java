package com.dcnm.donghaeng.server.trip;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** DB 표 destination_proposals: 목적지 제안. 수락한 차량 목록은 proposal_acceptances 표에 따로 저장된다. */
@Entity
@Table(name = "destination_proposals")
public class ProposalEntity {

    public enum Status { PENDING, CONFIRMED, REPLACED, CANCELLED }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 36)
    private String tripId;

    @Column(name = "proposer_id", nullable = false, length = 36)
    private String proposerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "proposal_acceptances", joinColumns = @JoinColumn(name = "proposal_id"))
    @Column(name = "member_id", length = 36)
    private Set<String> acceptedBy = new LinkedHashSet<>();

    protected ProposalEntity() {}

    public ProposalEntity(String id, String tripId, String proposerId, String name, double lat, double lng, Instant createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.proposerId = proposerId;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTripId() { return tripId; }
    public String getProposerId() { return proposerId; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public Status getStatus() { return status; }
    public Set<String> getAcceptedBy() { return acceptedBy; }

    public void setStatus(Status status) { this.status = status; }

    /** 처음 수락하면 true, 이미 수락했으면 false. */
    public boolean accept(String memberId) { return acceptedBy.add(memberId); }
}