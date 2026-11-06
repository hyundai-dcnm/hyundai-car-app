package com.dcnm.carapp.core.vehicle.driving

import com.dcnm.carapp.core.model.DrivingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenModePolicyTest {

    private fun kotlinx.coroutines.test.TestScope.collect(
        source: MutableStateFlow<DrivingState>,
    ): MutableList<ScreenMode> {
        val seen = mutableListOf<ScreenMode>()
        backgroundScope.launch { source.toScreenMode().collect { seen += it } }
        runCurrent()
        return seen
    }

    @Test
    fun `starting parked shows parked without delay`() = runTest {
        val seen = collect(MutableStateFlow(DrivingState.PARKED))
        assertEquals(listOf(ScreenMode.PARKED), seen)
    }

    @Test
    fun `entering driving is immediate`() = runTest {
        val source = MutableStateFlow(DrivingState.PARKED)
        val seen = collect(source)
        source.value = DrivingState.DRIVING
        runCurrent()
        assertEquals(listOf(ScreenMode.PARKED, ScreenMode.DRIVING), seen)
    }

    @Test
    fun `returning to parked waits one second`() = runTest {
        val source = MutableStateFlow(DrivingState.DRIVING)
        val seen = collect(source)
        source.value = DrivingState.PARKED
        advanceTimeBy(999); runCurrent()
        assertEquals(listOf(ScreenMode.DRIVING), seen)
        advanceTimeBy(1); runCurrent()
        assertEquals(listOf(ScreenMode.DRIVING, ScreenMode.PARKED), seen)
    }

    @Test
    fun `brief park blip does not switch screens`() = runTest {
        val source = MutableStateFlow(DrivingState.DRIVING)
        val seen = collect(source)
        source.value = DrivingState.PARKED
        advanceTimeBy(500); runCurrent()
        source.value = DrivingState.DRIVING
        advanceTimeBy(2_000); runCurrent()
        assertEquals(listOf(ScreenMode.DRIVING), seen)
    }

    @Test
    fun `unknown stays on driving then falls back to no-signal parked`() = runTest {
        val seen = collect(MutableStateFlow(DrivingState.UNKNOWN))
        assertEquals(listOf(ScreenMode.DRIVING), seen)
        advanceTimeBy(3_000); runCurrent()
        assertEquals(listOf(ScreenMode.DRIVING, ScreenMode.PARKED_NO_SIGNAL), seen)
    }

    @Test
    fun `initial mode matches policy`() {
        assertEquals(ScreenMode.PARKED, initialScreenMode(DrivingState.PARKED))
        assertEquals(ScreenMode.DRIVING, initialScreenMode(DrivingState.DRIVING))
        assertEquals(ScreenMode.DRIVING, initialScreenMode(DrivingState.UNKNOWN))
    }
}
