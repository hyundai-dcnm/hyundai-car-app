package com.dcnm.donghaeng.core.trip

import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeTripRepositoryTest {

    private val profile = Profile("자민", "자민네 차")
    private val beach = Destination("안목해변", LatLng(37.77, 128.95))

    @Test
    fun `turning sharing off changes only my car`() = runTest {
        val repo = FakeTripRepository(backgroundScope)
        repo.createTrip("강릉", profile)
        val before = repo.trip.value!!.members.associate { it.id to it.sharingLocation }

        repo.setLocationSharing(false)

        val after = repo.trip.value!!.members.associate { it.id to it.sharingLocation }
        val me = repo.myMemberId.value!!
        assertFalse(after.getValue(me))
        assertEquals(before - me, after - me)
    }

    @Test
    fun `proposed destination is confirmed after the others accept`() = runTest {
        val repo = FakeTripRepository(backgroundScope)
        repo.createTrip("강릉", profile)

        repo.proposeDestination(beach)
        assertNotNull(repo.trip.value!!.proposal)
        assertNull(repo.trip.value!!.destination)

        advanceTimeBy(2_001)
        runCurrent()

        assertEquals(beach, repo.trip.value!!.destination)
        assertNull(repo.trip.value!!.proposal)
    }

    @Test
    fun `leaving clears the trip`() = runTest {
        val repo = FakeTripRepository(backgroundScope)
        repo.createTrip("강릉", profile)

        repo.leaveTrip()

        assertNull(repo.trip.value)
        assertNull(repo.myMemberId.value)
        assertEquals(ConnectionStatus.Disconnected, repo.connection.value)
    }

    @Test
    fun `call needs dev credentials`() = runTest {
        val repo = FakeTripRepository(backgroundScope)
        repo.createTrip("강릉", profile)

        val result = runCatching { repo.callCredentials() }

        assertTrue(result.isFailure)
    }
}
