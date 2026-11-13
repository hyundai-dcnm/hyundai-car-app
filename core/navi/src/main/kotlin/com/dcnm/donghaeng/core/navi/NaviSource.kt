package com.dcnm.donghaeng.core.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pleos NaviHelper SDK 3.0.5 연동: 차량 내비의 목적지·안내 상태를 받고, 확정된 공동 목적지로 길 안내를 요청한다.
 *
 * 에뮬레이터(Pleos Connect 2.1.0)에서 확인한 동작:
 * - get*Info() 요청의 응답은 리스너를 등록한 모든 앱에 같이 간다. 그래서 주기적으로 요청하지 않고 이벤트 때만 다시 묻는다.
 * - 안내 중에도 RouteStateInfo.isRouting 이 false 로 온다 → naviMode 와 NavigationStatus 로 같이 판단한다(guiding).
 * - onRouteStarted 직후 onRouteEnded 가 바로 따라온다 → 종료 이벤트만으로 목적지를 지우지 않고 다시 조회한다.
 * - 안내 중이 아니면 DestinationInfo 가 빈 이름·주소로 온다 → 목적지 없음으로 처리한다.
 * - requestRoute() 를 부르면 순정 내비가 앞으로 나와 경로 선택 화면을 띄우고, 운전자가 "안내 시작"을 눌러야 안내가 시작된다.
 */
class NaviSource(context: Context) {

    private companion object { const val TAG = "DonghaengNavi" }

    private val helper = NaviHelper(context.applicationContext)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _destination = MutableStateFlow<NaviDestination?>(null)
    /** 내비에 설정된 목적지. 안내 중이 아니면 null */
    val destination: StateFlow<NaviDestination?> = _destination.asStateFlow()

    private val _guiding = MutableStateFlow(false)
    val guiding: StateFlow<Boolean> = _guiding.asStateFlow()

    private val _nextTurn = MutableStateFlow<NaviTurn?>(null)
    val nextTurn: StateFlow<NaviTurn?> = _nextTurn.asStateFlow()

    /** 내비가 현재 있다고 보는 주소(예: "수정구 시흥동"). 좌표는 LocationSource 를 쓴다 */
    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()

    private val _lastRequest = MutableStateFlow<RouteRequestResult?>(null)
    /** 마지막 길 안내 요청과, 내비에 실제로 반영됐는지 */
    val lastRequest: StateFlow<RouteRequestResult?> = _lastRequest.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var routeState: RouteStateInfo? = null
    private var status: NavigationStatus? = null

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) { _currentAddress.value = info.address.ifBlank { null } }
        override fun onNavigationStatus(status: NavigationStatus) { this@NaviSource.status = status; updateGuiding() }
        override fun onRouteStateInfo(info: RouteStateInfo) { routeState = info; updateGuiding() }
        override fun onDestinationInfo(info: DestinationInfo) { onDestination(info) }
        override fun onTBTInfo(list: List<TBTInfo>) {
            _nextTurn.value = list.firstOrNull()?.let { NaviTurn(it.type.name, it.distance, it.description) }
        }
        override fun onCurrentRoadSpeedLimit(limit: Int) = Unit
        override fun onRoutingCompleted() = refresh()
        override fun onRouteStarted(info: RouteStartInfo) { Log.d(TAG, "안내 시작: ${info.name}"); refresh() }
        override fun onRouteEnded() = refresh()
        override fun onRouteCancelled() { _destination.value = null; _nextTurn.value = null; refresh() }
        override fun onDestinationChanged() { runCatching { helper.getDestinationInfo() } }
        override fun onError(code: NaviErrorCode) { Log.w(TAG, "내비 오류: $code"); _lastError.value = code.name }
    }

    fun start() {
        runCatching {
            helper.initialize(
                {
                    _connected.value = true
                    helper.addListener(listener)
                    refresh()
                },
                { e -> _connected.value = false; _lastError.value = e.toString(); Log.w(TAG, "연결 실패", e) },
            )
        }.onFailure { _lastError.value = it.toString(); Log.w(TAG, "initialize 예외", it) }
    }

    /** 목적지·안내 상태·회전 안내·현재 주소를 다시 요청. 결과는 각 StateFlow 로 온다 */
    fun refresh() {
        if (!_connected.value) return
        runCatching {
            helper.getCurrentLocationInfo()
            helper.getRouteStateInfo()
            helper.getDestinationInfo()
            helper.getTBTInfo()
        }.onFailure { Log.w(TAG, "정보 요청 실패", it) }
    }

    /**
     * 확정된 공동 목적지로 내 차량 내비에 길 안내를 요청한다(서버 destination_confirmed 를 받았을 때).
     * 반영 여부는 lastRequest 로 확인한다.
     * @return 요청을 보냈으면 true. 내비 연결 전이면 false
     */
    fun requestRoute(destination: Destination): Boolean {
        if (!_connected.value) return false
        _lastRequest.value = RouteRequestResult(destination.location, destination.name, gapM = null)
        return runCatching {
            helper.requestRoute(
                RouteInfo(
                    latitude = destination.location.lat,
                    longitude = destination.location.lng,
                    displayLongitude = null,
                    displayLatitude = null,
                    poiName = destination.name,
                    poiId = "",
                    poiSubId = "",
                    address = destination.name,
                )
            )
        }.onFailure { _lastError.value = it.toString(); Log.w(TAG, "길 안내 요청 실패", it) }.isSuccess
    }

    fun cancelRoute() {
        if (!_connected.value) return
        runCatching { helper.cancelRoute() }.onFailure { Log.w(TAG, "경로 취소 실패", it) }
    }

    fun release() {
        runCatching { helper.removeListener(listener); helper.release() }
        _connected.value = false
    }

    private fun onDestination(info: DestinationInfo) {
        val d = info.takeIf { it.name.isNotBlank() || it.address.isNotBlank() }?.let {
            NaviDestination(it.name, it.address, LatLng(it.latitude, it.longitude), it.remainDistance, it.remainTime)
        }
        _destination.value = d
        val req = _lastRequest.value
        if (req != null && d != null) _lastRequest.value = req.copy(gapM = distanceM(req.requested, d.location))
    }

    private fun updateGuiding() {
        val rs = routeState
        _guiding.value = rs?.isRouting == true ||
            rs?.naviMode == NaviMode.ROUTE_GUIDING || rs?.naviMode == NaviMode.SIMULATION_GUIDING ||
            status == NavigationStatus.ROUTE_DRIVE
    }
}
