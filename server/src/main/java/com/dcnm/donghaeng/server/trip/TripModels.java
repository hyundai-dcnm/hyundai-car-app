package com.dcnm.donghaeng.server.trip;

import java.util.List;
import java.util.Set;

/** 앱(core/model)과 같은 JSON 모양. 바꾸면 contract/api.md 와 앱도 같이 바꾼다. */
public final class TripModels {
	private TripModels() {}

	public record LatLng(double lat, double lng) {}

	public record Destination(String name, LatLng location) {}

	public record Member(String id, String nickname, String carLabel, boolean sharingLocation) {
		Member withSharing(boolean sharing) {
			return new Member(id, nickname, carLabel, sharing);
		}
	}

	public record DestinationProposal(String id, String proposerId, Destination destination, Set<String> acceptedBy) {}

	public record Trip(
			String id,
			String name,
			String inviteCode,
			String hostId,
			List<Member> members,
			Destination destination,
			DestinationProposal proposal) {}

	public record MemberLocation(String memberId, LatLng location, Float headingDeg, Float speedKph, long updatedAtMillis) {}

	public record CallCredentials(String url, String token) {}

	public record JoinedTrip(Trip trip, String memberId, String accessToken) {}
}
