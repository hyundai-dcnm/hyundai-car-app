package com.dcnm.donghaeng.core.navi

import android.util.Log
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 여행방 목적지가 확정되면 내 차량 내비에 그 목적지로 길 안내를 요청한다.
 * - 같은 목적지는 한 번만 요청한다(재접속으로 snapshot 이 다시 와도 또 요청하지 않음).
 * - 내비가 이미 그 목적지로 설정돼 있으면 요청하지 않는다.
 * - 내비 연결 전에 확정되면 연결된 뒤 요청한다.
 * 요청하면 순정 내비가 앞으로 나와 경로 화면을 띄우고, 운전자가 "안내 시작"을 누른다.
 */
class DestinationAutoRouter(
    private val navi: NaviSource,
    private val scope: CoroutineScope,
) {
    private companion object {
        const val TAG = "DonghaengAutoRoute"
        /** 연결 직후 내비가 현재 목적지를 알려줄 때까지 기다리는 시간. 확정이 연달아 바뀔 때도 마지막 것만 요청한다 */
        const val SETTLE_MS = 1_500L
    }

    private var requested: Destination? = null

    /** @param confirmed 확정된 목적지. 확정 전이거나 방에서 나가면 null */
    fun bind(confirmed: Flow<Destination?>): Job = scope.launch {
        combine(confirmed.distinctUntilChanged(), navi.connected) { dest, connected -> dest to connected }
            .collectLatest { (dest, connected) ->
                if (dest == null) {
                    requested = null
                    return@collectLatest
                }
                if (!connected) return@collectLatest
                delay(SETTLE_MS)
                val current = navi.destination.value?.location
                if (!shouldRequestRoute(dest, requested, current)) return@collectLatest
                Log.d(TAG, "확정 목적지로 길 안내 요청: ${dest.name}")
                if (navi.requestRoute(dest)) requested = dest
            }
    }
}

/**
 * 확정된 목적지로 내비 길 안내를 요청해야 하는지.
 * @param alreadyRequested 이 앱이 이미 요청한 목적지
 * @param naviCurrent 내비에 지금 설정된 목적지 좌표
 */
fun shouldRequestRoute(confirmed: Destination?, alreadyRequested: Destination?, naviCurrent: LatLng?): Boolean {
    if (confirmed == null) return false
    val near = { p: LatLng -> distanceM(p, confirmed.location) <= RouteRequestResult.APPLIED_WITHIN_M }
    if (alreadyRequested != null && near(alreadyRequested.location)) return false
    if (naviCurrent != null && near(naviCurrent)) return false
    return true
}
