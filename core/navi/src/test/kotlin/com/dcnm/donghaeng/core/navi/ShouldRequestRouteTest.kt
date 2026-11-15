package com.dcnm.donghaeng.core.navi

import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldRequestRouteTest {

    private val gangneung = Destination("강릉 안목해변", LatLng(37.7717, 128.9476))
    private val sokcho = Destination("속초 영금정", LatLng(38.2133, 128.6002))

    @Test fun `확정 전에는 요청하지 않는다`() {
        assertFalse(shouldRequestRoute(null, null, null))
    }

    @Test fun `처음 확정되면 요청한다`() {
        assertTrue(shouldRequestRoute(gangneung, null, null))
    }

    @Test fun `이미 요청한 목적지는 다시 요청하지 않는다`() {
        assertFalse(shouldRequestRoute(gangneung, gangneung, null))
    }

    @Test fun `내비가 이미 그 목적지면 요청하지 않는다(입구 보정 150m 포함)`() {
        assertFalse(shouldRequestRoute(gangneung, null, LatLng(37.7717 + 0.00135, 128.9476)))
    }

    @Test fun `내비가 다른 곳으로 안내 중이면 요청한다`() {
        assertTrue(shouldRequestRoute(gangneung, null, sokcho.location))
    }

    @Test fun `목적지가 바뀌면 다시 요청한다`() {
        assertTrue(shouldRequestRoute(sokcho, gangneung, gangneung.location))
    }
}
