package com.dcnm.donghaeng.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.trip.ConnectionStatus
import com.dcnm.donghaeng.core.trip.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemberRow(
    val id: String,
    val nickname: String,
    val carLabel: String,
    val isMe: Boolean,
    val sharingLocation: Boolean,
    val inCall: Boolean,
    val speaking: Boolean,
)

data class ProposalCard(
    val id: String,
    val destination: Destination,
    val accepted: Int,
    val total: Int,
    val acceptedByMe: Boolean,
)

data class TripUiState(
    val tripName: String = "",
    val inviteCode: String = "",
    val connection: ConnectionStatus = ConnectionStatus.Disconnected,
    val members: List<MemberRow> = emptyList(),
    val destination: Destination? = null,
    val proposal: ProposalCard? = null,
    val sharingLocation: Boolean = true,
    val call: CallState = CallState.Idle,
    val message: String? = null,
)

class TripViewModel(
    private val trips: TripRepository,
    private val call: CallClient,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val ui: StateFlow<TripUiState> = combine(
        trips.trip, trips.myMemberId, trips.connection, call.state, message,
    ) { trip, me, connection, callState, msg ->
        if (trip == null) return@combine TripUiState()
        val inCall = (callState as? CallState.Connected)?.participants.orEmpty().associateBy { it.identity }
        TripUiState(
            tripName = trip.name,
            inviteCode = trip.inviteCode,
            connection = connection,
            members = trip.members.map {
                MemberRow(
                    id = it.id,
                    nickname = it.nickname,
                    carLabel = it.carLabel,
                    isMe = it.id == me,
                    sharingLocation = it.sharingLocation,
                    inCall = if (it.id == me) callState is CallState.Connected else it.id in inCall,
                    speaking = inCall[it.id]?.speaking == true,
                )
            },
            destination = trip.destination,
            proposal = trip.proposal?.let {
                ProposalCard(it.id, it.destination, it.acceptedBy.size, trip.members.size, me in it.acceptedBy)
            },
            sharingLocation = trip.members.firstOrNull { it.id == me }?.sharingLocation ?: false,
            call = callState,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripUiState())

    fun toggleLocationSharing() = act { trips.setLocationSharing(!ui.value.sharingLocation) }

    fun propose(destination: Destination) = act { trips.proposeDestination(destination) }

    fun accept(proposalId: String) = act { trips.acceptDestination(proposalId) }

    fun joinCall() = act { call.connect(trips.callCredentials()) }

    fun toggleMic() = act {
        val connected = call.state.value as? CallState.Connected ?: return@act
        call.setMicEnabled(!connected.micEnabled)
    }

    fun leaveCall() = act { call.disconnect() }

    fun leaveTrip() = act {
        call.disconnect()
        trips.leaveTrip()
    }

    fun dismissMessage() {
        message.value = null
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { message.value = it.message ?: "실패했어요" }
        }
    }

    companion object {
        /** 목적지 검색이 붙기 전까지 쓰는 시연용 후보. */
        val sampleDestinations = listOf(
            Destination("강릉 안목해변", LatLng(37.7717, 128.9476)),
            Destination("속초 영금정", LatLng(38.2133, 128.6002)),
            Destination("양양 서피비치", LatLng(38.0306, 128.7166)),
        )
    }
}
