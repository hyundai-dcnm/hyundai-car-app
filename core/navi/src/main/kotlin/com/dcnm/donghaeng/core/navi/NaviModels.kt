package com.dcnm.donghaeng.core.navi

import com.dcnm.donghaeng.core.model.LatLng
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 차량 내비에 지금 설정된 목적지.
 * @param address 내비가 주는 주소. 시·도가 빠져서 온다(예: "해운대구 우동", 에뮬레이터 확인).
 * @param remainDistanceM 남은 거리(m), @param remainTimeSec 남은 시간(초). 에뮬레이터에서 387000 / 17460 (387km, 4시간 51분) 확인.
 */
data class NaviDestination(
    val name: String,
    val address: String,
    val location: LatLng,
    val remainDistanceM: Int,
    val remainTimeSec: Int,
)

/** 다음 회전 안내. type 은 SDK 의 TurnType 이름(예: TURN_LEFT) */
data class NaviTurn(val type: String, val distanceM: Double, val description: String)

/**
 * 길 안내 요청 결과. 요청한 좌표와 내비가 실제로 잡은 목적지 좌표를 비교한다.
 * @param gapM 아직 내비가 목적지를 알려주지 않았으면 null
 */
data class RouteRequestResult(val requested: LatLng, val requestedName: String, val gapM: Double?) {
    val applied: Boolean? get() = gapM?.let { it <= APPLIED_WITHIN_M }

    companion object {
        /** 이 거리 안이면 요청한 목적지로 설정된 것으로 본다. 내비가 근처 도로·입구로 보정할 수 있어 여유를 둔다 */
        const val APPLIED_WITHIN_M = 200.0
    }
}

/** 두 좌표 사이 직선거리(m, 하버사인) */
fun distanceM(a: LatLng, b: LatLng): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}
