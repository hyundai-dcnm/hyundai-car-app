package com.dcnm.donghaeng.vehicle

import org.junit.Assert.*
import org.junit.Test

class VehiclePolicyTest {
    @Test fun `unknown and driving never unlock input`() {
        assertFalse(canConfigure(UxMode.Unknown)); assertFalse(canConfigure(UxMode.Restricted)); assertTrue(canConfigure(UxMode.Unrestricted))
    }
    @Test fun `only enabled same destination within radius is visible`() {
        val all = VehicleViewModel.candidates
        val off = NearbyPreview(destinationId = "demo-beach")
        assertTrue(off.candidates(all).isEmpty())
        assertEquals(listOf("demo-sky"), off.copy(enabled = true).candidates(all).map { it.id })
        assertTrue(off.copy(enabled = true, radiusM = 100).candidates(all).isEmpty())
        assertTrue(off.copy(enabled = true, destinationId = null).candidates(all).isEmpty())
    }
    @Test fun `outgoing request requires peer acceptance and destination change stops discovery`() {
        val waiting = NearbyPreview(enabled = true, destinationId = "demo-beach").send()
        assertEquals(NearbyRequest.Outgoing, waiting.request)
        assertEquals(NearbyRequest.Outgoing, waiting.acceptIncoming().request)
        assertEquals(NearbyRequest.Connected, waiting.peerAccepts().request)
        val changed = waiting.destination("other")
        assertFalse(changed.enabled); assertEquals(NearbyRequest.None, changed.request)
        assertEquals(NearbyRequest.None, changed.peerAccepts().request)
    }
    @Test fun `stop radius and block invalidate pending requests`() {
        val waiting = NearbyPreview(enabled = true, destinationId = "demo-beach").send()
        assertEquals(NearbyRequest.None, waiting.stop().peerAccepts().request)
        assertEquals(NearbyRequest.None, waiting.radius(1000).request)
        assertTrue(waiting.block("demo-sky").candidates(VehicleViewModel.candidates).isEmpty())
    }
}
