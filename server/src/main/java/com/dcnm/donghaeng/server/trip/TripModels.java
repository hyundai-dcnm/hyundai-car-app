package com.dcnm.donghaeng.server.trip;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 앱(core/model)·웹(types.ts)과 같은 JSON 형태
 * 바꿀 때는 contract/api.md 를 먼저 수정
 */
public final class TripModels {
    private TripModels() {}

    public record LatLng(
            @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @DecimalMin("-180.0") @DecimalMax("180.0") double lng) {}

    public record Destination(@NotBlank @Size(max = 100) String name, @NotNull @Valid LatLng location) {}

    public record Member(String id, String nickname, String carLabel, boolean sharingLocation) {}

    public record DestinationProposal(String id, String proposerId, Destination destination, List<String> acceptedBy) {}

    public record Trip(
            String id,
            String name,
            String inviteCode,
            String hostId,
            List<Member> members,
            Destination destination,
            DestinationProposal proposal) {}

    public record MemberLocation(String memberId, LatLng location, Double headingDeg, Double speedKph, long updatedAtMillis) {}

    public record CallCredentials(String url, String token) {}

    /** 차량이 방을 만들거나 참여했을 때 받는 것. */
    public record JoinedTrip(Trip trip, String memberId, String accessToken) {}

    /** 웹(앨범)으로 참여했을 때 받는 것. 차량 멤버가 아니라서 memberId 가 없음. */
    public record ViewerJoined(Trip trip, String displayName, String accessToken) {}

    public record Photo(String id, String uploaderName, String contentType, long sizeBytes, long createdAtMillis, String fileUrl) {}
}