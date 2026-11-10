package com.dcnm.donghaeng.core.location

import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.flow.Flow

/** 이 차량의 현재 위치. 수집하는 동안만 위치를 받는다. */
interface LocationSource {
    fun updates(): Flow<VehicleLocation>
}

data class VehicleLocation(
    val location: LatLng,
    val headingDeg: Float?,
    val speedKph: Float?,
)
