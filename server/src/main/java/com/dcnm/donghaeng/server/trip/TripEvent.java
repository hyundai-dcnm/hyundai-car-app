package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.Member;
import com.dcnm.donghaeng.server.trip.TripModels.MemberLocation;
import com.dcnm.donghaeng.server.trip.TripModels.Photo;
import com.dcnm.donghaeng.server.trip.TripModels.Trip;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/** WebSocket 으로 보내는 이벤트. JSON 의 "type" 값이 contract/api.md 의 type 과 같음. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(TripEvent.Snapshot.class),
        @JsonSubTypes.Type(TripEvent.MemberJoined.class),
        @JsonSubTypes.Type(TripEvent.MemberLeft.class),
        @JsonSubTypes.Type(TripEvent.LocationUpdated.class),
        @JsonSubTypes.Type(TripEvent.LocationSharingChanged.class),
        @JsonSubTypes.Type(TripEvent.DestinationProposed.class),
        @JsonSubTypes.Type(TripEvent.DestinationAccepted.class),
        @JsonSubTypes.Type(TripEvent.DestinationConfirmed.class),
        @JsonSubTypes.Type(TripEvent.PhotoAdded.class),
        @JsonSubTypes.Type(TripEvent.PhotoDeleted.class),
        @JsonSubTypes.Type(TripEvent.TripClosed.class),
})
public sealed interface TripEvent {

    @JsonTypeName("snapshot")
    record Snapshot(Trip trip, List<MemberLocation> locations) implements TripEvent {}

    @JsonTypeName("member_joined")
    record MemberJoined(Member member) implements TripEvent {}

    /** hostId 는 퇴장 후의 방장. 방장이 나가면 가장 먼저 들어온 차량이 이어받는다. */
    @JsonTypeName("member_left")
    record MemberLeft(String memberId, String hostId) implements TripEvent {}

    @JsonTypeName("location")
    record LocationUpdated(MemberLocation location) implements TripEvent {}

    @JsonTypeName("location_sharing")
    record LocationSharingChanged(String memberId, boolean sharing) implements TripEvent {}

    @JsonTypeName("destination_proposed")
    record DestinationProposed(DestinationProposal proposal) implements TripEvent {}

    @JsonTypeName("destination_accepted")
    record DestinationAccepted(String proposalId, String memberId) implements TripEvent {}

    @JsonTypeName("destination_confirmed")
    record DestinationConfirmed(Destination destination) implements TripEvent {}

    @JsonTypeName("photo_added")
    record PhotoAdded(Photo photo) implements TripEvent {}

    @JsonTypeName("photo_deleted")
    record PhotoDeleted(String photoId) implements TripEvent {}

    /** 마지막 차량이 나가 방이 닫히면 서버가 접속을 모두 끊는다. */
    @JsonTypeName("trip_closed")
    record TripClosed(String tripId) implements TripEvent {}
}