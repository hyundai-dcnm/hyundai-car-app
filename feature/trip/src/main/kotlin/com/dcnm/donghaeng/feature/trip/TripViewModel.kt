package com.dcnm.donghaeng.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.location.LocationSource
import com.dcnm.donghaeng.core.location.VehicleLocation
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.navi.NaviController
import com.dcnm.donghaeng.core.navi.NaviResult
import com.dcnm.donghaeng.core.network.ConnectionStatus
import com.dcnm.donghaeng.core.network.TripRepository
import com.dcnm.donghaeng.feature.map.MapPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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
    val pins: List<MapPin> = emptyList(),
    val destination: Destination? = null,
    val proposal: ProposalCard? = null,
    val sharingLocation: Boolean = true,
    val call: CallState = CallState.Idle,
    val message: String? = null,
)

class TripViewModel(
    private val trips: TripRepository,
    private val call: CallClient,
    location: LocationSource,
    private val navi: NaviController,
) : ViewModel() {

    private val myLocation = MutableStateFlow<VehicleLocation?>(null)
    private val message = MutableStateFlow<String?>(null)

    val ui: StateFlow<TripUiState> = combine(
        trips.state, trips.myMemberId, trips.connection, call.state, combine(myLocation, message, ::Pair),
    ) { state, me, connection, callState, (mine, msg) ->
        if (state == null) return@combine TripUiState()
        val trip = state.trip
        val inCall = (callState as? CallState.Connected)?.participants.orEmpty().associateBy { it.identity }
        val iAmInCall = callState is CallState.Connected
        val pins = trip.members.mapNotNull { member ->
            val at = if (member.id == me) mine?.location else state.locations[member.id]?.location
            at?.let { MapPin(member.id, member.carLabel, it, isMe = member.id == me) }
        }
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
                    inCall = if (it.id == me) iAmInCall else it.id in inCall,
                    speaking = inCall[it.id]?.speaking == true,
                )
            },
            pins = pins,
            destination = trip.destination,
            proposal = trip.proposal?.let {
                ProposalCard(it.id, it.destination, it.acceptedBy.size, trip.members.size, me in it.acceptedBy)
            },
            sharingLocation = trip.members.firstOrNull { it.id == me }?.sharingLocation ?: false,
            call = callState,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripUiState())

    init {
        viewModelScope.launch {
            location.updates().collect {
                myLocation.value = it
                runCatching { trips.sendLocation(it.location, it.headingDeg, it.speedKph) }
            }
        }
        // 목적지가 확정되면 각 차량이 자기 내비에 안내를 요청한다.
        viewModelScope.launch {
            trips.state.map { it?.trip?.destination }.filterNotNull().distinctUntilChanged().collect { dest ->
                when (val result = navi.navigateTo(dest)) {
                    NaviResult.Requested -> message.value = "${dest.name}(으)로 길 안내를 시작해요"
                    is NaviResult.Failed -> message.value = "내비 연결 실패: ${result.reason}"
                }
            }
        }
    }

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
        /** 목적지 검색이 붙기 전까지 쓰는 시연용 후보. TODO(①): 내비 검색으로 교체 */
        val sampleDestinations = listOf(
            Destination("강릉 안목해변", LatLng(37.7717, 128.9476)),
            Destination("속초 영금정", LatLng(38.2133, 128.6002)),
            Destination("양양 서피비치", LatLng(38.0306, 128.7166)),
        )
    }
}
