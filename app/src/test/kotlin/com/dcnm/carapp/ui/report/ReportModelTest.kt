package com.dcnm.carapp.ui.report

import com.dcnm.carapp.core.model.SignalSample
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.ui.parked.demoSymptomEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportModelTest {

    private fun snapshot(samples: List<SignalSample>) = SignalSnapshot(0, 30_000, 10_000, 40_000, samples)
    private fun s(t: Long, type: String, v: String) = SignalSample(t, type, v, "", SignalSource.PLEOS_SDK)

    @Test
    fun `missing signals are reported as absent, not as normal`() {
        val lines = observations(snapshot(listOf(s(0, "speed", "30"))))
        assertTrue("브레이크 신호 없음" in lines)
        assertTrue("조향 신호 없음" in lines)
    }

    @Test
    fun `brake peak is placed relative to the utterance`() {
        val lines = observations(snapshot(listOf(s(-3_000, "brake", "82"), s(0, "brake", "10"))))
        assertTrue(lines.any { it == "브레이크 입력 최대 82% (녹음 3초 전)" })
    }

    @Test
    fun `gap at window start is detected`() {
        val lane = buildLanes(demoSymptomEvent().snapshot).first { it.type == "speed" }
        val g = gaps(lane, -30f, 10f)
        assertEquals(1, g.size)
        assertEquals(-30f, g.first().start)
    }

    @Test
    fun `samples outside the window are ignored`() {
        val lane = buildLanes(snapshot(listOf(s(-60_000, "speed", "10")))).first { it.type == "speed" }
        assertEquals(listOf(-30f..10f), gaps(lane, -30f, 10f))
    }

    @Test
    fun `lane without samples is one full gap`() {
        val lane = buildLanes(snapshot(emptyList())).first()
        assertEquals(listOf(-30f..10f), gaps(lane, -30f, 10f))
    }

    @Test
    fun `short coverage is stated`() {
        assertEquals("차량 신호 36초 확보 (요청 40초 중)", coverageLine(snapshot(emptyList()).copy(coveredMillis = 36_000)))
    }
}
