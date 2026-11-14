package com.dcnm.donghaeng.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcnm.donghaeng.server.auth.AccessTokenService;
import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.LatLng;
import com.dcnm.donghaeng.server.trip.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

/** 서버 규칙 확인. 메모리 DB 로 돌아서 내 컴퓨터 data 폴더를 건드리지 않는다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "storage.type=local"
})
class TripFlowTest {

    @Autowired
    TripService trips;

    @Autowired
    AccessTokenService tokens;

    @Test
    void destinationIsConfirmedWhenEveryCarAccepts() {
        var host = trips.create("강릉 여행", "민지", "민지네 차");
        var guest = trips.join(host.trip().inviteCode(), "규원", "규원이 차");
        var hostMe = tokens.resolve(host.accessToken()).orElseThrow();
        var guestMe = tokens.resolve(guest.accessToken()).orElseThrow();

        var proposal = trips.propose(hostMe, new Destination("강릉 안목해변", new LatLng(37.7717, 128.9476)));
        assertThat(trips.trip(hostMe).destination()).isNull();
        assertThat(trips.trip(hostMe).proposal().acceptedBy()).containsExactly(host.memberId());

        trips.accept(guestMe, proposal.id());

        var trip = trips.trip(hostMe);
        assertThat(trip.destination().name()).isEqualTo("강릉 안목해변");
        assertThat(trip.proposal()).isNull();
    }

    @Test
    void leavingRevokesTokenAndPassesHost() {
        var host = trips.create("속초 여행", "민지", "민지네 차");
        var guest = trips.join(host.trip().inviteCode(), "규원", "규원이 차");

        trips.leave(tokens.resolve(host.accessToken()).orElseThrow());

        assertThat(tokens.resolve(host.accessToken())).isEmpty();
        var guestMe = tokens.resolve(guest.accessToken()).orElseThrow();
        assertThat(trips.trip(guestMe).hostId()).isEqualTo(guest.memberId());
        assertThat(trips.trip(guestMe).members()).hasSize(1);
    }

    @Test
    void closedTripCannotBeJoined() {
        var host = trips.create("양양 여행", "민지", "민지네 차");
        trips.leave(tokens.resolve(host.accessToken()).orElseThrow());

        assertThatThrownBy(() -> trips.join(host.trip().inviteCode(), "규원", "규원이 차"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void viewerCannotProposeDestination() {
        var host = trips.create("부산 여행", "민지", "민지네 차");
        var viewer = trips.joinAsViewer(host.trip().inviteCode(), "엄마");
        var viewerMe = tokens.resolve(viewer.accessToken()).orElseThrow();

        assertThatThrownBy(() -> trips.propose(viewerMe, new Destination("해운대", new LatLng(35.1587, 129.1604))))
                .isInstanceOf(ResponseStatusException.class);
    }
}