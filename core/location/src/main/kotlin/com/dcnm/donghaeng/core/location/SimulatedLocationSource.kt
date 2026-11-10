package com.dcnm.donghaeng.core.location

import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 에뮬레이터·화면 개발용. 서울시청에서 동쪽으로 1초에 한 번 움직인다. */
class SimulatedLocationSource : LocationSource {
    override fun updates(): Flow<VehicleLocation> = flow {
        var step = 0
        while (true) {
            emit(VehicleLocation(LatLng(37.5665, 126.978 + step * 0.002), headingDeg = 90f, speedKph = 72f))
            step++
            delay(1_000)
        }
    }
}
