package com.dcnm.donghaeng.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 여행방 WebSocket으로 오가는 이벤트. JSON의 "type" 필드로 구분한다.
 * 형식은 contract/api.md 와 같아야 한다.
 */
@Serializable
sealed interface TripEvent {

    /** 방 전체 상태. 접속 직후와 재접속 직후 서버가 한 번 보낸다. */
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(val trip: Trip, val locations: List<MemberLocation>) : TripEvent

    @Serializable
    @SerialName("member_joined")
    data class MemberJoined(val member: Member) : TripEvent

    @Serializable
    @SerialName("member_left")
    data class MemberLeft(val memberId: String) : TripEvent

    @Serializable
    @SerialName("location")
    data class LocationUpdated(val location: MemberLocation) : TripEvent

    @Serializable
    @SerialName("location_sharing")
    data class LocationSharingChanged(val memberId: String, val sharing: Boolean) : TripEvent

    @Serializable
    @SerialName("destination_proposed")
    data class DestinationProposed(val proposal: DestinationProposal) : TripEvent

    @Serializable
    @SerialName("destination_accepted")
    data class DestinationAccepted(val proposalId: String, val memberId: String) : TripEvent

    @Serializable
    @SerialName("destination_confirmed")
    data class DestinationConfirmed(val destination: Destination) : TripEvent
}
