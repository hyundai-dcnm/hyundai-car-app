package com.dcnm.carapp.core.navi

import com.dcnm.carapp.core.model.ServiceShop

/**
 * 정비소 경유지 재계획. NaviHelper 3.0.5 + FusedLocation 1.1.3.
 *
 * 흐름: 현재 위치(FusedLocation.getLastKnownLocation) → 번들 정비소 중 가까운 3곳 →
 *       requestTrip 으로 ETA 비교 → addWaypoint 로 경유지 추가.
 *
 * 제약:
 * - POI 텍스트 검색 API는 3rd party에 닫혀 있다. 주행 중에는 번들 좌표만 쓰고, 정차 중 자유 검색은
 *   requestNavigationScreen(SearchPlace) 화면 위임으로 처리한다.
 * - 경유지는 최대 2개. 추가 실패를 성공으로 표시하지 않는다.
 * - 에뮬레이터 내비는 진행형 이벤트(TBT·도착)를 내지 않으므로 도착 판정은 기어 P로 대체한다.
 */
interface NaviRepository {
    suspend fun nearbyShops(limit: Int = 3): List<ShopCandidate>
    suspend fun addWaypoint(shop: ServiceShop): WaypointResult
    suspend fun openSearchScreen(query: String)
}

data class ShopCandidate(val shop: ServiceShop, val distanceMeters: Double?, val etaSeconds: Int?)

sealed interface WaypointResult {
    data class Added(val shop: ServiceShop) : WaypointResult
    data class Failed(val reason: String) : WaypointResult
}
