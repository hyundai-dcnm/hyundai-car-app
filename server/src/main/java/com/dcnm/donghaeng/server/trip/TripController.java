package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.call.CallTokenService;
import com.dcnm.donghaeng.server.trip.TripModels.CallCredentials;
import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.JoinedTrip;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** contract/api.md 의 REST 부분. */
@RestController
@RequestMapping("/api/trips")
public class TripController {

	private final TripService trips;
	private final CallTokenService callTokens;

	public TripController(TripService trips, CallTokenService callTokens) {
		this.trips = trips;
		this.callTokens = callTokens;
	}

	public record CreateTripRequest(@NotBlank String name, @NotBlank String nickname, @NotBlank String carLabel) {}

	public record JoinTripRequest(@NotBlank String inviteCode, @NotBlank String nickname, @NotBlank String carLabel) {}

	public record SharingRequest(boolean sharing) {}

	public record ProposeRequest(@NotNull Destination destination) {}

	@PostMapping
	public JoinedTrip create(@Valid @RequestBody CreateTripRequest body) {
		return trips.create(body.name(), body.nickname(), body.carLabel());
	}

	@PostMapping("/join")
	public JoinedTrip join(@Valid @RequestBody JoinTripRequest body) {
		return trips.join(body.inviteCode(), body.nickname(), body.carLabel());
	}

	@DeleteMapping("/{tripId}/members/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable String tripId, @RequestHeader("Authorization") String auth) {
		trips.leave(session(tripId, auth));
	}

	@PutMapping("/{tripId}/members/me/sharing")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void setSharing(@PathVariable String tripId, @RequestHeader("Authorization") String auth,
			@RequestBody SharingRequest body) {
		trips.setSharing(session(tripId, auth), body.sharing());
	}

	@PostMapping("/{tripId}/proposals")
	public DestinationProposal propose(@PathVariable String tripId, @RequestHeader("Authorization") String auth,
			@Valid @RequestBody ProposeRequest body) {
		return trips.propose(session(tripId, auth), body.destination());
	}

	@PostMapping("/{tripId}/proposals/{proposalId}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void accept(@PathVariable String tripId, @PathVariable String proposalId,
			@RequestHeader("Authorization") String auth) {
		trips.accept(session(tripId, auth), proposalId);
	}

	@PostMapping("/{tripId}/call-token")
	public CallCredentials callToken(@PathVariable String tripId, @RequestHeader("Authorization") String auth) {
		var s = session(tripId, auth);
		var member = trips.trip(tripId).members().stream()
				.filter(m -> m.id().equals(s.memberId())).findFirst().orElseThrow();
		return callTokens.issue(tripId, member.id(), member.carLabel());
	}

	private TripService.Session session(String tripId, String authorization) {
		String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
		return trips.session(token)
				.filter(s -> s.tripId().equals(tripId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이 방에 들어와 있지 않아요"));
	}
}
