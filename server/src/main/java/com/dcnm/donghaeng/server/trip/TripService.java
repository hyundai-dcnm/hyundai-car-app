package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.auth.AccessTokenService;
import com.dcnm.donghaeng.server.auth.TripPrincipal;
import com.dcnm.donghaeng.server.call.CallTokenService;
import com.dcnm.donghaeng.server.location.LocationRelay;
import com.dcnm.donghaeng.server.trip.TripModels.CallCredentials;
import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.JoinedTrip;
import com.dcnm.donghaeng.server.trip.TripModels.LatLng;
import com.dcnm.donghaeng.server.trip.TripModels.Member;
import com.dcnm.donghaeng.server.trip.TripModels.MemberLocation;
import com.dcnm.donghaeng.server.trip.TripModels.Trip;
import com.dcnm.donghaeng.server.trip.TripModels.ViewerJoined;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 여행방의 모든 규칙.
 * 바뀐 내용은 publish() 로 이벤트를 내고, DB 저장이 끝나면 TripSocketHandler 가 방 접속자에게 보낸다.
 */
@Service
public class TripService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** 한 여행방에 들어올 수 있는 차량 수. */
    private static final int MAX_CARS = 6;

    private final SecureRandom random = new SecureRandom();
    private final TripRepository trips;
    private final MemberRepository members;
    private final ProposalRepository proposals;
    private final AccessTokenService tokens;
    private final LocationRelay locations;
    private final CallTokenService callTokens;
    private final ApplicationEventPublisher events;

    public TripService(TripRepository trips, MemberRepository members, ProposalRepository proposals,
                       AccessTokenService tokens, LocationRelay locations, CallTokenService callTokens,
                       ApplicationEventPublisher events) {
        this.trips = trips;
        this.members = members;
        this.proposals = proposals;
        this.tokens = tokens;
        this.locations = locations;
        this.callTokens = callTokens;
        this.events = events;
    }

    // ───────── 방 만들기/참여/나가기 ─────────

    @Transactional
    public JoinedTrip create(String name, String nickname, String carLabel) {
        Instant now = Instant.now();
        TripEntity trip = trips.save(new TripEntity(newId(), name.trim(), newInviteCode(), now));
        MemberEntity host = members.save(new MemberEntity(newId(), trip.getId(), nickname.trim(), carLabel.trim(), now));
        trip.setHostMemberId(host.getId());
        String token = tokens.issueForCar(trip.getId(), host.getId(), host.getCarLabel());
        return new JoinedTrip(toTrip(trip), host.getId(), token);
    }

    @Transactional
    public JoinedTrip join(String inviteCode, String nickname, String carLabel) {
        TripEntity trip = lockOpenTrip(openTripByCode(inviteCode).getId());
        if (activeMembers(trip.getId()).size() >= MAX_CARS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "여행방이 가득 찼어요 (최대 " + MAX_CARS + "대)");
        }
        MemberEntity member = members.save(
                new MemberEntity(newId(), trip.getId(), nickname.trim(), carLabel.trim(), Instant.now()));
        String token = tokens.issueForCar(trip.getId(), member.getId(), member.getCarLabel());
        publish(trip.getId(), new TripEvent.MemberJoined(toMember(member)));
        return new JoinedTrip(toTrip(trip), member.getId(), token);
    }

    /** 웹 앨범 참여. 차량 목록에 들어가지 않고 목적지 수락 인원에서 세지 않는다. */
    @Transactional
    public ViewerJoined joinAsViewer(String inviteCode, String nickname) {
        TripEntity trip = openTripByCode(inviteCode);
        String name = nickname.trim();
        String token = tokens.issueForViewer(trip.getId(), name);
        return new ViewerJoined(toTrip(trip), name, token);
    }

    @Transactional
    public void leave(TripPrincipal me) {
        requireCar(me);
        TripEntity trip = lockOpenTrip(me.tripId());
        MemberEntity member = activeMember(trip.getId(), me.memberId());
        Instant now = Instant.now();
        member.leave(now);
        tokens.revokeMember(trip.getId(), member.getId());
        locations.remove(trip.getId(), member.getId());

        List<MemberEntity> rest = activeMembers(trip.getId());
        if (rest.isEmpty()) {
            trip.close(now);
            proposals.findFirstByTripIdAndStatus(trip.getId(), ProposalEntity.Status.PENDING)
                    .ifPresent(p -> p.setStatus(ProposalEntity.Status.CANCELLED));
            tokens.revokeTrip(trip.getId());
            locations.removeTrip(trip.getId());
            publish(trip.getId(), new TripEvent.TripClosed(trip.getId()));
            return;
        }
        if (member.getId().equals(trip.getHostMemberId())) {
            trip.setHostMemberId(rest.get(0).getId());
        }
        publish(trip.getId(), new TripEvent.MemberLeft(member.getId(), trip.getHostMemberId()));
        // 나간 차량만 수락을 안 했던 경우, 남은 차량 기준으로 목적지가 확정될 수 있다.
        confirmIfEveryoneAccepted(trip, rest);
    }

    // ───────── 조회 ─────────

    @Transactional(readOnly = true)
    public Trip trip(TripPrincipal me) {
        return toTrip(openTrip(me.tripId()));
    }

    @Transactional(readOnly = true)
    public TripEvent.Snapshot snapshot(String tripId) {
        return new TripEvent.Snapshot(toTrip(openTrip(tripId)), locations.list(tripId));
    }

    @Transactional(readOnly = true)
    public boolean isHost(String tripId, String memberId) {
        return memberId != null && trips.findById(tripId).map(t -> memberId.equals(t.getHostMemberId())).orElse(false);
    }

    // ───────── 위치 공유 ─────────

    @Transactional
    public void setSharing(TripPrincipal me, boolean sharing) {
        requireCar(me);
        MemberEntity member = activeMember(me.tripId(), me.memberId());
        member.setSharingLocation(sharing);
        if (!sharing) locations.remove(me.tripId(), me.memberId());
        publish(me.tripId(), new TripEvent.LocationSharingChanged(me.memberId(), sharing));
    }

    /** WebSocket 으로 들어온 위치. 공유를 끈 차량 및 퇴장한 차량의 위치는 버린다. */
    @Transactional(readOnly = true)
    public void relayLocation(TripPrincipal me, LatLng location, Double headingDeg, Double speedKph) {
        if (me.role() != TripPrincipal.Role.CAR || location == null) return;
        boolean sharing = members.findByIdAndTripIdAndLeftAtIsNull(me.memberId(), me.tripId())
                .map(MemberEntity::isSharingLocation)
                .orElse(false);
        if (!sharing) return;
        var stamped = new MemberLocation(me.memberId(), location, headingDeg, speedKph, System.currentTimeMillis());
        if (locations.put(me.tripId(), stamped)) {
            publish(me.tripId(), new TripEvent.LocationUpdated(stamped));
        }
    }

    // ───────── 목적지 제안/수락 ─────────

    @Transactional
    public DestinationProposal propose(TripPrincipal me, Destination destination) {
        requireCar(me);
        TripEntity trip = lockOpenTrip(me.tripId());
        activeMember(trip.getId(), me.memberId());
        // 새 제안이 오면 기다리던 제안은 무효.
        proposals.findFirstByTripIdAndStatus(trip.getId(), ProposalEntity.Status.PENDING)
                .ifPresent(old -> old.setStatus(ProposalEntity.Status.REPLACED));
        ProposalEntity proposal = proposals.save(new ProposalEntity(newId(), trip.getId(), me.memberId(),
                destination.name().trim(), destination.location().lat(), destination.location().lng(), Instant.now()));
        proposal.accept(me.memberId()); // 제안한 차량은 수락한 것으로 본다.
        DestinationProposal dto = toProposal(proposal);
        publish(trip.getId(), new TripEvent.DestinationProposed(dto));
        confirmIfEveryoneAccepted(trip, activeMembers(trip.getId()));
        return dto;
    }

    @Transactional
    public void accept(TripPrincipal me, String proposalId) {
        requireCar(me);
        TripEntity trip = lockOpenTrip(me.tripId());
        activeMember(trip.getId(), me.memberId());
        ProposalEntity proposal = proposals.findFirstByTripIdAndStatus(trip.getId(), ProposalEntity.Status.PENDING)
                .filter(p -> p.getId().equals(proposalId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "지난 제안이에요"));
        if (proposal.accept(me.memberId())) {
            publish(trip.getId(), new TripEvent.DestinationAccepted(proposalId, me.memberId()));
        }
        confirmIfEveryoneAccepted(trip, activeMembers(trip.getId()));
    }

    // ───────── 통화 ─────────

    @Transactional(readOnly = true)
    public CallCredentials callCredentials(TripPrincipal me) {
        requireCar(me);
        MemberEntity member = activeMember(me.tripId(), me.memberId());
        return callTokens.issue(me.tripId(), member.getId(), member.getCarLabel());
    }

    // ───────── 내부 헬퍼 ─────────

    /** 남아 있는 차량이 모두 수락했으면 목적지를 확정하고 알린다. 이때 각 앱은 자기 내비에 길 안내를 요청한다. */
    private void confirmIfEveryoneAccepted(TripEntity trip, List<MemberEntity> active) {
        proposals.findFirstByTripIdAndStatus(trip.getId(), ProposalEntity.Status.PENDING).ifPresent(p -> {
            boolean everyone = active.stream().allMatch(m -> p.getAcceptedBy().contains(m.getId()));
            if (!everyone) return;
            p.setStatus(ProposalEntity.Status.CONFIRMED);
            trip.confirmDestination(p.getName(), p.getLat(), p.getLng());
            publish(trip.getId(), new TripEvent.DestinationConfirmed(
                    new Destination(p.getName(), new LatLng(p.getLat(), p.getLng()))));
        });
    }

    private Trip toTrip(TripEntity trip) {
        List<Member> memberList = activeMembers(trip.getId()).stream().map(TripService::toMember).toList();
        DestinationProposal proposal = proposals.findFirstByTripIdAndStatus(trip.getId(), ProposalEntity.Status.PENDING)
                .map(TripService::toProposal)
                .orElse(null);
        Destination destination = trip.getDestinationName() == null ? null
                : new Destination(trip.getDestinationName(), new LatLng(trip.getDestinationLat(), trip.getDestinationLng()));
        return new Trip(trip.getId(), trip.getName(), trip.getInviteCode(), trip.getHostMemberId(), memberList,
                destination, proposal);
    }

    private static Member toMember(MemberEntity m) {
        return new Member(m.getId(), m.getNickname(), m.getCarLabel(), m.isSharingLocation());
    }

    private static DestinationProposal toProposal(ProposalEntity p) {
        return new DestinationProposal(p.getId(), p.getProposerId(),
                new Destination(p.getName(), new LatLng(p.getLat(), p.getLng())), List.copyOf(p.getAcceptedBy()));
    }

    private TripEntity openTripByCode(String inviteCode) {
        String code = inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT);
        return trips.findByInviteCodeAndClosedAtIsNull(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대 코드가 없어요"));
    }

    private TripEntity openTrip(String tripId) {
        return trips.findById(tripId).filter(t -> !t.isClosed())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행방이 없어요"));
    }

    private TripEntity lockOpenTrip(String tripId) {
        return trips.findByIdForUpdate(tripId).filter(t -> !t.isClosed())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행방이 없어요"));
    }

    private MemberEntity activeMember(String tripId, String memberId) {
        return members.findByIdAndTripIdAndLeftAtIsNull(memberId, tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "이 여행방에 들어와 있지 않아요"));
    }

    private List<MemberEntity> activeMembers(String tripId) {
        return members.findByTripIdAndLeftAtIsNullOrderByJoinedAtAsc(tripId);
    }

    private static void requireCar(TripPrincipal me) {
        if (me.role() != TripPrincipal.Role.CAR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차량 앱에서만 할 수 있어요");
        }
    }

    private void publish(String tripId, TripEvent event) {
        events.publishEvent(new TripEventMessage(tripId, event));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    /** 헷갈리는 글자(0·O·1·I)를 뺀 6자리. 차 안에서 말로 부르기 쉽도록 한다. */
    private String newInviteCode() {
        String code;
        do {
            var sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (trips.existsByInviteCode(code));
        return code;
    }
}