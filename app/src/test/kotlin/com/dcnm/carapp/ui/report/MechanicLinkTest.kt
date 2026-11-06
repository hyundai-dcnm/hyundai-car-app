package com.dcnm.carapp.ui.report

import com.dcnm.carapp.ui.parked.demoSymptomEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MechanicLinkTest {

    private val event = demoSymptomEvent(now = 1_700_000_000_000)

    @Test
    fun `payload survives encode and decode`() {
        val payload = MechanicLink.payload(event, sameTriggerCount = 2, latestMaintenance = null)
        val decoded = Json.parseToJsonElement(MechanicLink.decode(MechanicLink.encode(payload.toString())))
        assertEquals(payload, decoded)
    }

    @Test
    fun `one bin per second, gaps stay null instead of interpolated`() {
        val speed = MechanicLink.bins(event, "speed")
        assertEquals(MechanicLink.TO_SEC - MechanicLink.FROM_SEC + 1, speed.size)
        // 데모는 말하기 26초 전부터만 확보 → -30..-27초 칸은 비어 있다.
        assertTrue(speed.take(4).all { it is JsonNull })
        assertEquals(82, MechanicLink.bins(event, "brake")[-3 - MechanicLink.FROM_SEC].jsonPrimitive.int)
    }

    @Test
    fun `gear and odometer are taken at the utterance`() {
        val p = MechanicLink.payload(event, 0, null).jsonObject
        assertEquals("D", p["gear"]!!.jsonPrimitive.content)
        assertEquals(48_213, p["odo"]!!.jsonPrimitive.int)
        assertTrue(p["obs"]!!.jsonArray.isNotEmpty())
    }

    @Test
    fun `link stays small enough for a car-screen QR`() {
        val url = MechanicLink.url("https://hyundai-dcnm.github.io/hyundai-car-app/mechanic/", MechanicLink.payload(event, 3, null))
        // QR 버전 25(L) 바이트 용량 ≈ 1,200. 차량 화면 640dp에서 휴대폰으로 무리 없이 찍히는 범위.
        assertTrue("url length ${url.length}", url.length < 1_200)
    }
}
