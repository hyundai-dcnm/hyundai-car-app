package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.Member;
import com.dcnm.donghaeng.server.trip.TripModels.MemberLocation;
import com.dcnm.donghaeng.server.trip.TripModels.Trip;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/** WebSocket 이벤트. JSON "type" 필드 값은 앱의 @SerialName 과 같다. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = TripEvent.Snapshot.class, name = "snapshot"),
		@JsonSubTypes.Type(value = TripEvent.MemberJoined.class, name = "member_joined"),
		@JsonSubTypes.Type(value = TripEvent.MemberLeft.class, name = "member_left"),
		@JsonSubTypes.Type(value = TripEvent.LocationUpdated.class, name = "location"),
		@JsonSubTypes.Type(value = TripEvent.LocationSharingChanged.class, name = "location_sharing"),
		@JsonSubTypes.Type(value = TripEvent.DestinationProposed.class, name = "destination_proposed"),
		@JsonSubTypes.Type(value = TripEvent.DestinationAccepted.class, name = "destination_accepted"),
		@JsonSubTypes.Type(value = TripEvent.DestinationConfirmed.class, name = "destination_confirmed"),
})
public sealed interface TripEvent {
	record Snapshot(Trip trip, List<MemberLocation> locations) implements TripEvent {}

	record MemberJoined(Member member) implements TripEvent {}

	record MemberLeft(String memberId) implements TripEvent {}

	record LocationUpdated(MemberLocation location) implements TripEvent {}

	record LocationSharingChanged(String memberId, boolean sharing) implements TripEvent {}

	record DestinationProposed(DestinationProposal proposal) implements TripEvent {}

	record DestinationAccepted(String proposalId, String memberId) implements TripEvent {}

	record DestinationConfirmed(Destination destination) implements TripEvent {}
}
