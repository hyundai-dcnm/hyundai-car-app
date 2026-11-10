package com.dcnm.donghaeng.core.trip

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.DestinationProposal
import com.dcnm.donghaeng.core.model.Member
import com.dcnm.donghaeng.core.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 서버 없이 화면을 만들기 위한 가짜 방. 일행 두 대가 들어와 있고,
 * 목적지를 제안하면 2초 뒤 모두 수락한다.
 * 통화는 devCall 이 있을 때만 된다 (모든 기기가 같은 LiveKit 방 토큰을 써야 서로 들린다).
 */
class FakeTripRepository(
    private val scope: CoroutineScope,
    private val devCall: CallCredentials? = null,
) : TripRepository {

    private val _trip = MutableStateFlow<Trip?>(null)
    override val trip: StateFlow<Trip?> = _trip

    private val _myMemberId = MutableStateFlow<String?>(null)
    override val myMemberId: StateFlow<String?> = _myMemberId

    private val _connection = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connection: StateFlow<ConnectionStatus> = _connection

    private val friends = listOf(
        Member("fake-2", "민지", "민지네 차"),
        Member("fake-3", "규원", "규원이 차", sharingLocation = false),
    )

    override suspend fun createTrip(name: String, profile: Profile) = enter(name, profile)

    override suspend fun joinTrip(inviteCode: String, profile: Profile) = enter("$inviteCode 여행방", profile)

    private fun enter(name: String, profile: Profile) {
        val me = Member("fake-1", profile.nickname, profile.carLabel)
        _myMemberId.value = me.id
        _trip.value = Trip("fake-trip", name, "DEMO42", hostId = me.id, members = listOf(me) + friends)
        _connection.value = ConnectionStatus.Connected
    }

    override suspend fun leaveTrip() {
        _trip.value = null
        _myMemberId.value = null
        _connection.value = ConnectionStatus.Disconnected
    }

    override suspend fun setLocationSharing(sharing: Boolean) {
        val me = _myMemberId.value ?: return
        _trip.update { trip ->
            trip?.copy(members = trip.members.map { if (it.id == me) it.copy(sharingLocation = sharing) else it })
        }
    }

    override suspend fun proposeDestination(destination: Destination) {
        val me = _myMemberId.value ?: return
        val proposal = DestinationProposal("fake-proposal-${System.currentTimeMillis()}", me, destination, setOf(me))
        _trip.update { it?.copy(proposal = proposal) }
        scope.launch {
            delay(2_000)
            _trip.update { it?.copy(destination = destination, proposal = null) }
        }
    }

    override suspend fun acceptDestination(proposalId: String) {
        val me = _myMemberId.value ?: return
        _trip.update { trip ->
            val proposal = trip?.proposal?.takeIf { it.id == proposalId } ?: return@update trip
            trip.copy(proposal = proposal.copy(acceptedBy = proposal.acceptedBy + me))
        }
    }

    override suspend fun callCredentials(): CallCredentials =
        devCall ?: error("통화 토큰이 없어요. donghaeng.livekitUrl / donghaeng.livekitToken 을 넣어 주세요")
}
