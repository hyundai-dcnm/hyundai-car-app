package com.dcnm.donghaeng.core.navi

import android.content.Context
import com.dcnm.donghaeng.core.model.Destination

/**
 * Pleos NaviHelper 어댑터. 담당: ①
 *
 * TODO(①): NaviHelper(context).initialize() 후 목적지 설정·addWaypoint 호출.
 *  11/09 에뮬레이터 확인: addWaypoint 는 동작하지만 경로가 약 7초 취소됐다가 다시 잡힌다.
 */
class PleosNaviController(private val context: Context) : NaviController {
    override suspend fun navigateTo(destination: Destination): NaviResult = NaviResult.Failed("not implemented")
    override suspend fun addWaypoint(destination: Destination): NaviResult = NaviResult.Failed("not implemented")
}
