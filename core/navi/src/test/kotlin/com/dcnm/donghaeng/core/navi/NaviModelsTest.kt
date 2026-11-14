package com.dcnm.donghaeng.core.navi

import com.dcnm.donghaeng.core.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NaviModelsTest {

    @Test fun `같은 좌표는 0m`() {
        val p = LatLng(35.158698, 129.160384)
        assertEquals(0.0, distanceM(p, p), 1e-6)
    }

    @Test fun `서울역-부산역 직선거리는 약 325km`() {
        val seoul = LatLng(37.5547, 126.9707)
        val busan = LatLng(35.1151, 129.0422)
        assertEquals(325_000.0, distanceM(seoul, busan), 5_000.0)
    }

    @Test fun `위도 0_001도는 약 111m`() {
        assertEquals(111.2, distanceM(LatLng(37.0, 127.0), LatLng(37.001, 127.0)), 0.5)
    }

    @Test fun `내비 응답 전에는 반영 여부를 모른다`() {
        assertNull(RouteRequestResult(LatLng(0.0, 0.0), "x", gapM = null).applied)
    }

    @Test fun `200m 이내면 반영, 넘으면 미반영`() {
        assertTrue(RouteRequestResult(LatLng(0.0, 0.0), "x", gapM = 0.0).applied!!)
        assertTrue(RouteRequestResult(LatLng(0.0, 0.0), "x", gapM = 200.0).applied!!)
        assertFalse(RouteRequestResult(LatLng(0.0, 0.0), "x", gapM = 201.0).applied!!)
    }
}
