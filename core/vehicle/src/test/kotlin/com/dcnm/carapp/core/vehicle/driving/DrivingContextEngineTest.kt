package com.dcnm.carapp.core.vehicle.driving

import com.dcnm.carapp.core.model.DrivingState
import com.dcnm.carapp.core.model.GearPosition
import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.core.model.VehicleSignal
import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingContextEngineTest {

    private fun gear(g: GearPosition) = VehicleSignal.Gear(g, 0, SignalSource.AAOS_CAR_API)
    private fun speed(kmh: Float) = VehicleSignal.Speed(kmh, 0, SignalSource.AAOS_CAR_API)

    @Test
    fun `starts unknown until any input arrives`() {
        assertEquals(DrivingState.UNKNOWN, DrivingContextEngine().state.value)
    }

    @Test
    fun `ux restrictions decide state when available`() {
        val engine = DrivingContextEngine()
        engine.onUxRestrictionsChanged(true)
        assertEquals(DrivingState.DRIVING, engine.state.value)
        engine.onUxRestrictionsChanged(false)
        assertEquals(DrivingState.PARKED, engine.state.value)
    }

    @Test
    fun `ux restrictions override gear signal`() {
        val engine = DrivingContextEngine()
        engine.onUxRestrictionsChanged(true)
        engine.onSignal(gear(GearPosition.P))
        assertEquals(DrivingState.DRIVING, engine.state.value)
    }

    @Test
    fun `falls back to gear and speed when restrictions unavailable`() {
        val engine = DrivingContextEngine()
        engine.onUxRestrictionsChanged(null)
        engine.onSignal(gear(GearPosition.P))
        assertEquals(DrivingState.PARKED, engine.state.value)
        engine.onSignal(gear(GearPosition.D))
        assertEquals(DrivingState.DRIVING, engine.state.value)
    }

    @Test
    fun `P gear above 5kmh is still driving`() {
        val engine = DrivingContextEngine()
        engine.onSignal(gear(GearPosition.P))
        engine.onSignal(speed(6f))
        assertEquals(DrivingState.DRIVING, engine.state.value)
    }
}
