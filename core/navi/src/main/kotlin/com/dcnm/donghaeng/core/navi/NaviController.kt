package com.dcnm.donghaeng.core.navi

import com.dcnm.donghaeng.core.model.Destination

/** 차량 순정 내비에 길 안내를 요청한다. */
interface NaviController {
    suspend fun navigateTo(destination: Destination): NaviResult
    suspend fun addWaypoint(destination: Destination): NaviResult
}

sealed interface NaviResult {
    data object Requested : NaviResult
    data class Failed(val reason: String) : NaviResult
}
