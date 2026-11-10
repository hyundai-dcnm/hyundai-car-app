package com.dcnm.donghaeng.core.location

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Pleos FusedLocation 어댑터. 담당: ①
 *
 * TODO(①): FusedLocation 초기화 → isSupported() 확인 → 위치 콜백을 callbackFlow 로 감싸기.
 *  getLastKnownLocation() 은 null 을 줄 수 있다. 수집이 끝나면 콜백 해제.
 */
class PleosLocationSource(private val context: Context) : LocationSource {
    override fun updates(): Flow<VehicleLocation> = emptyFlow()
}
