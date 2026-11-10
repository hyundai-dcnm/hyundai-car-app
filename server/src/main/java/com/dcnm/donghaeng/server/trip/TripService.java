package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.JoinedTrip;
import com.dcnm.donghaeng.server.trip.TripModels.Member;
import com.dcnm.donghaeng.server.trip.TripModels.MemberLocation;
import com.dcnm.donghaeng.server.trip.TripModels.Trip;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 여행방 상태. 지금은 메모리에만 둔다. 담당: ④
 *
 * TODO(④): PostgreSQL(JPA)로 옮기기. 위치는 저장하지 않고 중계만 한다(마지막 위치만 메모리에).
 */
@Service
public class TripService {

	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final SecureRandom random = new SecureRandom();
	private final Map<String, Trip> trips = new ConcurrentHashMap<>();
	private final Map<String, String> tripIdByCode = new ConcurrentHashMap<>();
	private final Map<String, Session> sessions = new ConcurrentHashMap<>();
	private final Map<String, Map<String, MemberLocation>> locations = new ConcurrentHashMap<>();
	private final TripBroadcaster broadcaster;

	public TripService(TripBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	/** accessToken 으로 찾은 접속자. */
	public record Session(String tripId, String memberId) {}

	public synchronized JoinedTrip create(String name, String nickname, String carLabel) {
		String code = newInviteCode();
		Member host = newMember(nickname, carLabel);
		Trip trip = new Trip(UUID.randomUUID().toString(), name, code, host.id(), List.of(host), null, null);
		trips.put(trip.id(), trip);
		tripIdByCode.put(code, trip.id());
		return enter(trip, host);
	}

	public synchronized JoinedTrip join(String inviteCode, String nickname, String carLabel) {
		String tripId = tripIdByCode.get(inviteCode.trim().toUpperCase());
		if (tripId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "초대 코드가 없어요");
		Member member = newMember(nickname, carLabel);
		Trip trip = update(tripId, t -> withMembers(t, append(t.members(), member)));
		broadcaster.send(tripId, new TripEvent.MemberJoined(member));
		return enter(trip, member);
	}

	public synchronized void leave(Session s) {
		update(s.tripId(), t -> withMembers(t, t.members().stream().filter(m -> !m.id().equals(s.memberId())).toList()));
		locationsOf(s.tripId()).remove(s.memberId());
		sessions.values().removeIf(it -> it.equals(s));
		broadcaster.send(s.tripId(), new TripEvent.MemberLeft(s.memberId()));
	}

	public synchronized void setSharing(Session s, boolean sharing) {
		update(s.tripId(), t -> withMembers(t, t.members().stream()
				.map(m -> m.id().equals(s.memberId()) ? m.withSharing(sharing) : m).toList()));
		if (!sharing) locationsOf(s.tripId()).remove(s.memberId());
		broadcaster.send(s.tripId(), new TripEvent.LocationSharingChanged(s.memberId(), sharing));
	}

	public synchronized DestinationProposal propose(Session s, Destination destination) {
		var proposal = new DestinationProposal(UUID.randomUUID().toString(), s.memberId(), destination, Set.of(s.memberId()));
		update(s.tripId(), t -> new Trip(t.id(), t.name(), t.inviteCode(), t.hostId(), t.members(), t.destination(), proposal));
		broadcaster.send(s.tripId(), new TripEvent.DestinationProposed(proposal));
		confirmIfEveryoneAccepted(s.tripId());
		return proposal;
	}

	public synchronized void accept(Session s, String proposalId) {
		Trip trip = trip(s.tripId());
		DestinationProposal p = trip.proposal();
		if (p == null || !p.id().equals(proposalId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "지난 제안이에요");
		var accepted = new HashSet<>(p.acceptedBy());
		accepted.add(s.memberId());
		var updated = new DestinationProposal(p.id(), p.proposerId(), p.destination(), Set.copyOf(accepted));
		update(s.tripId(), t -> new Trip(t.id(), t.name(), t.inviteCode(), t.hostId(), t.members(), t.destination(), updated));
		broadcaster.send(s.tripId(), new TripEvent.DestinationAccepted(proposalId, s.memberId()));
		confirmIfEveryoneAccepted(s.tripId());
	}

	/** 위치 공유를 끈 차량이 보낸 위치는 버린다. */
	public void updateLocation(Session s, MemberLocation location) {
		Trip trip = trips.get(s.tripId());
		if (trip == null) return;
		boolean sharing = trip.members().stream().anyMatch(m -> m.id().equals(s.memberId()) && m.sharingLocation());
		if (!sharing) return;
		var stamped = new MemberLocation(s.memberId(), location.location(), location.headingDeg(), location.speedKph(),
				System.currentTimeMillis());
		locationsOf(s.tripId()).put(s.memberId(), stamped);
		broadcaster.send(s.tripId(), new TripEvent.LocationUpdated(stamped));
	}

	public TripEvent.Snapshot snapshot(String tripId) {
		return new TripEvent.Snapshot(trip(tripId), List.copyOf(locationsOf(tripId).values()));
	}

	public Optional<Session> session(String accessToken) {
		return Optional.ofNullable(accessToken).map(sessions::get);
	}

	public Trip trip(String tripId) {
		Trip trip = trips.get(tripId);
		if (trip == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "여행방이 없어요");
		return trip;
	}

	private void confirmIfEveryoneAccepted(String tripId) {
		Trip t = trip(tripId);
		DestinationProposal p = t.proposal();
		if (p == null) return;
		boolean everyone = t.members().stream().allMatch(m -> p.acceptedBy().contains(m.id()));
		if (!everyone) return;
		trips.put(tripId, new Trip(t.id(), t.name(), t.inviteCode(), t.hostId(), t.members(), p.destination(), null));
		broadcaster.send(tripId, new TripEvent.DestinationConfirmed(p.destination()));
	}

	private JoinedTrip enter(Trip trip, Member member) {
		String token = UUID.randomUUID().toString();
		sessions.put(token, new Session(trip.id(), member.id()));
		return new JoinedTrip(trip, member.id(), token);
	}

	private Trip update(String tripId, java.util.function.UnaryOperator<Trip> change) {
		return trips.compute(tripId, (id, t) -> {
			if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "여행방이 없어요");
			return change.apply(t);
		});
	}

	private Map<String, MemberLocation> locationsOf(String tripId) {
		return locations.computeIfAbsent(tripId, id -> new ConcurrentHashMap<>());
	}

	private static Trip withMembers(Trip t, List<Member> members) {
		return new Trip(t.id(), t.name(), t.inviteCode(), t.hostId(), members, t.destination(), t.proposal());
	}

	private static List<Member> append(List<Member> members, Member member) {
		var list = new ArrayList<>(members);
		list.add(member);
		return List.copyOf(list);
	}

	private Member newMember(String nickname, String carLabel) {
		return new Member(UUID.randomUUID().toString(), nickname, carLabel, true);
	}

	private String newInviteCode() {
		String code;
		do {
			var sb = new StringBuilder();
			for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
			code = sb.toString();
		} while (tripIdByCode.containsKey(code));
		return code;
	}
}
