package com.dcnm.carapp.core.vehicle.buffer

import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.core.model.VehicleSignal
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalRingBufferTest {

    private fun brake(t: Long, p: Int) = VehicleSignal.BrakePedal(p, t, SignalSource.PLEOS_SDK)

    @Test
    fun `cut returns only samples inside window and reports covered length`() {
        val buffer = SignalRingBuffer(retentionMillis = 60_000)
        (0..50).forEach { i -> buffer.append(brake(i * 1_000L, i)) }

        val snap = buffer.cut(anchorMillis = 40_000, beforeMillis = 30_000, afterMillis = 5_000)

        assertEquals(36, snap.samples.size)          // 10s..45s inclusive
        assertEquals(35_000, snap.coveredMillis)
        assertEquals("10", snap.samples.first().value)
    }

    @Test
    fun `old samples are evicted`() {
        val buffer = SignalRingBuffer(retentionMillis = 10_000)
        buffer.append(brake(0, 0))
        buffer.append(brake(20_000, 1))
        assertEquals(1, buffer.size())
    }

    @Test
    fun `short buffer reports short coverage instead of padding`() {
        val buffer = SignalRingBuffer()
        buffer.append(brake(100_000, 0))
        buffer.append(brake(103_000, 50))
        val snap = buffer.cut(anchorMillis = 103_000)
        assertEquals(3_000, snap.coveredMillis)
    }
}
