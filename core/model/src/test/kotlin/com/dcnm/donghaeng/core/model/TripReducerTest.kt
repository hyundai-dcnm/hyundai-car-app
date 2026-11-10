package com.dcnm.donghaeng.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TripReducerTest {

    private val me = Member("m1", "자민", "자민네 차")
    private val friend = Member("m2", "민지", "민지네 차")
    private val state = TripState(Trip("t1", "강릉", "ABC123", hostId = "m1", members = listOf(me, friend)))
    private val seoul = LatLng(37.56, 126.97)

    @Test
    fun `turning sharing off hides the last location`() {
        val located = state.reduce(TripEvent.LocationUpdated(MemberLocation("m2", seoul, updatedAtMillis = 0)))

        val hidden = located.reduce(TripEvent.LocationSharingChanged("m2", sharing = false))

        assertNull(hidden.locations["m2"])
        assertFalse(hidden.trip.members.first { it.id == "m2" }.sharingLocation)
    }

    @Test
    fun `confirmed destination clears the proposal`() {
        val dest = Destination("안목해변", LatLng(37.77, 128.95))
        val proposed = state.reduce(TripEvent.DestinationProposed(DestinationProposal("p1", "m1", dest)))
            .reduce(TripEvent.DestinationAccepted("p1", "m2"))

        assertEquals(setOf("m2"), proposed.trip.proposal?.acceptedBy)

        val confirmed = proposed.reduce(TripEvent.DestinationConfirmed(dest))
        assertEquals(dest, confirmed.trip.destination)
        assertNull(confirmed.trip.proposal)
    }

    @Test
    fun `events round-trip with a type field`() {
        val json = Json { classDiscriminator = "type" }
        val event: TripEvent = TripEvent.MemberLeft("m2")

        val text = json.encodeToString(event)

        assertEquals("""{"type":"member_left","memberId":"m2"}""", text)
        assertEquals(event, json.decodeFromString<TripEvent>(text))
    }
}
