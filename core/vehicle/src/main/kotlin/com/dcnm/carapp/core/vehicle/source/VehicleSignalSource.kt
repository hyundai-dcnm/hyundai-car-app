package com.dcnm.carapp.core.vehicle.source

import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow

/**
 * 차량 신호 공급자. 구현체는 Pleos SDK, AAOS Car API, 디버그 재생기 세 가지.
 *
 * 모든 구현은 수신 즉시 [VehicleSignal]로 변환해 내보내며, 주파수를 보장하지 않는다.
 * 실제 전달 주기는 샘플의 epochMillis로 사후에 계산한다.
 */
interface VehicleSignalSource {
    val signals: Flow<VehicleSignal>

    /** SDK 연결·리스너 등록. 실패하면 예외 대신 false를 돌려 상위에서 폴백하게 한다. */
    suspend fun start(): Boolean

    suspend fun stop()
}
