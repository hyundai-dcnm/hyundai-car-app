package com.dcnm.donghaeng.feature.trip

import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.CallParticipant
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.DestinationProposal
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.model.Member
import com.dcnm.donghaeng.core.model.Trip
import com.dcnm.donghaeng.core.trip.ConnectionStatus
import com.dcnm.donghaeng.core.trip.Profile
import com.dcnm.donghaeng.core.trip.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {

    private val me = Member("m1", "자민", "자민네 차")
    private val minji = Member("m2", "민지", "민지네 차")
    private val gyuwon = Member("m3", "규원", "규원이 차", sharingLocation = false)
    private val beach = Destination("안목해변", LatLng(37.77, 128.95))

    private val trips = StubTrips(Trip("t1", "강릉", "ABC123", hostId = me.id, members = listOf(me, minji, gyuwon)))
    private val call = StubCall()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = TripViewModel(trips, call).also { vm ->
        // ui 는 구독자가 있을 때만 계산된다.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
    }

    @Test
    fun `call participants are matched to members by id`() = runTest {
        val vm = viewModel()

        call.state.value = CallState.Connected(
            micEnabled = true,
            participants = listOf(CallParticipant("m2", "민지네 차", speaking = true)),
        )

        val rows = vm.ui.value.members.associateBy { it.id }
        assertTrue(rows.getValue("m1").inCall)
        assertTrue(rows.getValue("m2").inCall)
        assertTrue(rows.getValue("m2").speaking)
        assertFalse(rows.getValue("m3").inCall)
        assertFalse(rows.getValue("m3").sharingLocation)
    }

    @Test
    fun `proposal card counts accepted cars`() = runTest {
        val vm = viewModel()
        trips.update { it.copy(proposal = DestinationProposal("p1", "m2", beach, setOf("m2"))) }

        assertEquals(1, vm.ui.value.proposal!!.accepted)
        assertEquals(3, vm.ui.value.proposal!!.total)
        assertFalse(vm.ui.value.proposal!!.acceptedByMe)

        vm.accept("p1")

        assertEquals(2, vm.ui.value.proposal!!.accepted)
        assertTrue(vm.ui.value.proposal!!.acceptedByMe)
    }

    @Test
    fun `mute toggles the microphone`() = runTest {
        val vm = viewModel()
        vm.joinCall()
        assertTrue((vm.ui.value.call as CallState.Connected).micEnabled)

        vm.toggleMic()

        assertFalse((vm.ui.value.call as CallState.Connected).micEnabled)
    }

    @Test
    fun `failing to get a call token shows a message and stays idle`() = runTest {
        trips.credentials = null
        val vm = viewModel()

        vm.joinCall()

        assertEquals(CallState.Idle, vm.ui.value.call)
        assertNotNull(vm.ui.value.message)
    }

    @Test
    fun `leaving the trip also ends the call`() = runTest {
        val vm = viewModel()
        vm.joinCall()

        vm.leaveTrip()

        assertEquals(CallState.Idle, call.state.value)
        assertEquals(null, trips.trip.value)
    }

    private class StubTrips(initial: Trip) : TripRepository {
        override val trip = MutableStateFlow<Trip?>(initial)
        override val myMemberId: StateFlow<String?> = MutableStateFlow("m1")
        override val connection: StateFlow<ConnectionStatus> = MutableStateFlow(ConnectionStatus.Connected)
        var credentials: CallCredentials? = CallCredentials("wss://test", "token")

        fun update(change: (Trip) -> Trip) = trip.update { it?.let(change) }

        override suspend fun createTrip(name: String, profile: Profile) = Unit
        override suspend fun joinTrip(inviteCode: String, profile: Profile) = Unit
        override suspend fun leaveTrip() {
            trip.value = null
        }

        override suspend fun setLocationSharing(sharing: Boolean) = Unit
        override suspend fun proposeDestination(destination: Destination) = Unit
        override suspend fun acceptDestination(proposalId: String) = update { t ->
            t.copy(proposal = t.proposal?.let { it.copy(acceptedBy = it.acceptedBy + "m1") })
        }

        override suspend fun callCredentials() = credentials ?: error("토큰 없음")
    }

    private class StubCall : CallClient {
        override val state = MutableStateFlow<CallState>(CallState.Idle)

        override suspend fun connect(credentials: CallCredentials) {
            state.value = CallState.Connected(micEnabled = true, participants = emptyList())
        }

        override suspend fun setMicEnabled(enabled: Boolean) {
            state.update { (it as? CallState.Connected)?.copy(micEnabled = enabled) ?: it }
        }

        override suspend fun disconnect() {
            state.value = CallState.Idle
        }
    }
}
