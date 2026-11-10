package com.dcnm.donghaeng.core.network

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.DestinationProposal
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.model.Member
import com.dcnm.donghaeng.core.model.MemberLocation
import com.dcnm.donghaeng.core.model.Trip
import com.dcnm.donghaeng.core.model.TripEvent
import com.dcnm.donghaeng.core.model.TripState
import com.dcnm.donghaeng.core.model.reduce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 서버 없이 화면을 만들기 위한 가짜 방. 일행 두 대가 서울에서 동쪽으로 움직이고,
 * 목적지를 제안하면 2초 뒤 모두 수락한다. 통화는 되지 않는다.
 */
class FakeTripRepository(private val scope: CoroutineScope) : TripRepository {

    private val _state = MutableStateFlow<TripState?>(null)
    override val state: StateFlow<TripState?> = _state

    private val _myMemberId = MutableStateFlow<String?>(null)
    override val myMemberId: StateFlow<String?> = _myMemberId

    private val _connection = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connection: StateFlow<ConnectionStatus> = _connection

    private var simulation: Job? = null

    private val friends = listOf(
        Member("fake-2", "민지", "민지네 차"),
        Member("fake-3", "규원", "규원이 차"),
    )

    override suspend fun createTrip(name: String, profile: Profile) = enter(name, profile)

    override suspend fun joinTrip(inviteCode: String, profile: Profile) = enter("$inviteCode 여행방", profile)

    private fun enter(name: String, profile: Profile) {
        val me = Member("fake-1", profile.nickname, profile.carLabel)
        _myMemberId.value = me.id
        _state.value = TripState(Trip("fake-trip", name, "DEMO42", hostId = me.id, members = listOf(me) + friends))
        _connection.value = ConnectionStatus.Connected
        simulation?.cancel()
        simulation = scope.launch { moveFriends() }
    }

    private suspend fun moveFriends() {
        var step = 0
        while (currentCoroutineContext().isActive) {
            friends.forEachIndexed { i, friend ->
                val location = LatLng(37.55 + i * 0.02, 126.98 + step * 0.002 + i * 0.01)
                apply(TripEvent.LocationUpdated(MemberLocation(friend.id, location, 90f, 80f, System.currentTimeMillis())))
            }
            step++
            delay(1_000)
        }
    }

    override suspend fun leaveTrip() {
        simulation?.cancel()
        _state.value = null
        _myMemberId.value = null
        _connection.value = ConnectionStatus.Disconnected
    }

    override suspend fun setLocationSharing(sharing: Boolean) {
        apply(TripEvent.LocationSharingChanged(_myMemberId.value ?: return, sharing))
    }

    override suspend fun sendLocation(location: LatLng, headingDeg: Float?, speedKph: Float?) {
        val me = _myMemberId.value ?: return
        val sharing = _state.value?.trip?.members?.firstOrNull { it.id == me }?.sharingLocation ?: false
        if (sharing) apply(TripEvent.LocationUpdated(MemberLocation(me, location, headingDeg, speedKph, System.currentTimeMillis())))
    }

    override suspend fun proposeDestination(destination: Destination) {
        val me = _myMemberId.value ?: return
        val proposal = DestinationProposal("fake-proposal-${System.currentTimeMillis()}", me, destination, setOf(me))
        apply(TripEvent.DestinationProposed(proposal))
        scope.launch {
            delay(2_000)
            friends.forEach { apply(TripEvent.DestinationAccepted(proposal.id, it.id)) }
            apply(TripEvent.DestinationConfirmed(destination))
        }
    }

    override suspend fun acceptDestination(proposalId: String) {
        val me = _myMemberId.value ?: return
        apply(TripEvent.DestinationAccepted(proposalId, me))
        val proposal = _state.value?.trip?.proposal ?: return
        if (proposal.acceptedBy.containsAll(_state.value!!.trip.members.map { it.id })) {
            apply(TripEvent.DestinationConfirmed(proposal.destination))
        }
    }

    override suspend fun callCredentials(): CallCredentials =
        error("가짜 서버는 통화 토큰을 주지 않는다. -Pdonghaeng.apiBaseUrl 로 실제 서버를 지정한다.")

    private fun apply(event: TripEvent) = _state.update { it?.reduce(event) }
}
