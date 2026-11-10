package com.dcnm.donghaeng.core.model

/** 서버 이벤트를 받아 클라이언트가 들고 있는 방 상태를 갱신한다. 서버와 앱이 같은 규칙을 쓴다. */
data class TripState(
    val trip: Trip,
    val locations: Map<String, MemberLocation> = emptyMap(),
)

fun TripState.reduce(event: TripEvent): TripState = when (event) {
    is TripEvent.Snapshot -> TripState(event.trip, event.locations.associateBy { it.memberId })
    is TripEvent.MemberJoined -> copy(
        trip = trip.copy(members = trip.members.filterNot { it.id == event.member.id } + event.member),
    )
    is TripEvent.MemberLeft -> copy(
        trip = trip.copy(members = trip.members.filterNot { it.id == event.memberId }),
        locations = locations - event.memberId,
    )
    is TripEvent.LocationUpdated -> copy(locations = locations + (event.location.memberId to event.location))
    is TripEvent.LocationSharingChanged -> copy(
        trip = trip.copy(
            members = trip.members.map { if (it.id == event.memberId) it.copy(sharingLocation = event.sharing) else it },
        ),
        // 공유를 끈 차량의 마지막 위치는 바로 지운다.
        locations = if (event.sharing) locations else locations - event.memberId,
    )
    is TripEvent.DestinationProposed -> copy(trip = trip.copy(proposal = event.proposal))
    is TripEvent.DestinationAccepted -> {
        val proposal = trip.proposal
        if (proposal == null || proposal.id != event.proposalId) this
        else copy(trip = trip.copy(proposal = proposal.copy(acceptedBy = proposal.acceptedBy + event.memberId)))
    }
    is TripEvent.DestinationConfirmed -> copy(trip = trip.copy(destination = event.destination, proposal = null))
}
