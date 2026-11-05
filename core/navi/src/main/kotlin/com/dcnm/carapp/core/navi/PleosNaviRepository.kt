package com.dcnm.carapp.core.navi

import android.content.Context
import com.dcnm.carapp.core.model.ServiceShop

/**
 * NaviHelper/FusedLocation 어댑터. 1주차 스파이크 6~8번(addWaypoint poiId 빈 값, requestTrip 응답 속도, 좌표 반환)에서 채운다.
 */
class PleosNaviRepository(
    private val context: Context,
    private val shops: BundledShopProvider,
) : NaviRepository {

    // TODO(spike-6~8): NaviHelper(context).initialize(); addListener(...) / FusedLocation 초기화

    override suspend fun nearbyShops(limit: Int): List<ShopCandidate> {
        // TODO: 현재 위치 → 직선거리 정렬 → 상위 limit → requestTrip ETA (합계 3초 넘으면 1건만)
        return shops.all().take(limit).map { ShopCandidate(it, null, null) }
    }

    override suspend fun addWaypoint(shop: ServiceShop): WaypointResult {
        // TODO: naviHelper.addWaypoint(lat, lon, name, poiId = "") → 콜백 결과로 Added/Failed
        return WaypointResult.Failed("not implemented")
    }

    override suspend fun openSearchScreen(query: String) {
        // TODO: naviHelper.requestNavigationScreen(SearchPlace(query)) — 정차 중에만 호출
    }
}
