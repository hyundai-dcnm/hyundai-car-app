package com.dcnm.donghaeng.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.LatLng;
import com.dcnm.donghaeng.server.trip.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TripFlowTest {

	@Autowired
	TripService trips;

	@Test
	void destinationIsConfirmedWhenEveryCarAccepts() {
		var host = trips.create("강릉", "자민", "자민네 차");
		var guest = trips.join(host.trip().inviteCode(), "민지", "민지네 차");
		var hostSession = trips.session(host.accessToken()).orElseThrow();
		var guestSession = trips.session(guest.accessToken()).orElseThrow();

		var proposal = trips.propose(hostSession, new Destination("안목해변", new LatLng(37.77, 128.95)));
		assertThat(trips.trip(host.trip().id()).destination()).isNull();

		trips.accept(guestSession, proposal.id());

		var trip = trips.trip(host.trip().id());
		assertThat(trip.destination().name()).isEqualTo("안목해변");
		assertThat(trip.proposal()).isNull();
	}
}
