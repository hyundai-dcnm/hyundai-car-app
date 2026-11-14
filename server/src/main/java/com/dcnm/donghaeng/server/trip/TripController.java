package com.dcnm.donghaeng.server.trip;

import com.dcnm.donghaeng.server.auth.TripPrincipal;
import com.dcnm.donghaeng.server.trip.TripModels.CallCredentials;
import com.dcnm.donghaeng.server.trip.TripModels.Destination;
import com.dcnm.donghaeng.server.trip.TripModels.DestinationProposal;
import com.dcnm.donghaeng.server.trip.TripModels.JoinedTrip;
import com.dcnm.donghaeng.server.trip.TripModels.Trip;
import com.dcnm.donghaeng.server.trip.TripModels.ViewerJoined;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** contract/api.md 의 REST 부분(여행방). */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService trips;

    public TripController(TripService trips) {
        this.trips = trips;
    }

    public record CreateTripRequest(
            @NotBlank @Size(max = 30) String name,
            @NotBlank @Size(max = 20) String nickname,
            @NotBlank @Size(max = 20) String carLabel) {}

    public record JoinTripRequest(
            @NotBlank @Size(min = 6, max = 6) String inviteCode,
            @NotBlank @Size(max = 20) String nickname,
            @NotBlank @Size(max = 20) String carLabel) {}

    public record ViewerJoinRequest(
            @NotBlank @Size(min = 6, max = 6) String inviteCode,
            @NotBlank @Size(max = 20) String nickname) {}

    public record SharingRequest(boolean sharing) {}

    public record ProposeRequest(@NotNull @Valid Destination destination) {}

    @PostMapping
    public JoinedTrip create(@Valid @RequestBody CreateTripRequest body) {
        return trips.create(body.name(), body.nickname(), body.carLabel());
    }

    @PostMapping("/join")
    public JoinedTrip join(@Valid @RequestBody JoinTripRequest body) {
        return trips.join(body.inviteCode(), body.nickname(), body.carLabel());
    }

    @PostMapping("/web-join")
    public ViewerJoined webJoin(@Valid @RequestBody ViewerJoinRequest body) {
        return trips.joinAsViewer(body.inviteCode(), body.nickname());
    }

    @GetMapping("/{tripId}")
    public Trip get(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me) {
        return trips.trip(me.inTrip(tripId));
    }

    @DeleteMapping("/{tripId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me) {
        trips.leave(me.inTrip(tripId));
    }

    @PutMapping("/{tripId}/members/me/sharing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setSharing(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me,
                           @RequestBody SharingRequest body) {
        trips.setSharing(me.inTrip(tripId), body.sharing());
    }

    @PostMapping("/{tripId}/proposals")
    public DestinationProposal propose(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me,
                                       @Valid @RequestBody ProposeRequest body) {
        return trips.propose(me.inTrip(tripId), body.destination());
    }

    @PostMapping("/{tripId}/proposals/{proposalId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable String tripId, @PathVariable String proposalId,
                       @AuthenticationPrincipal TripPrincipal me) {
        trips.accept(me.inTrip(tripId), proposalId);
    }

    @PostMapping("/{tripId}/call-token")
    public CallCredentials callToken(@PathVariable String tripId, @AuthenticationPrincipal TripPrincipal me) {
        return trips.callCredentials(me.inTrip(tripId));
    }
}