package com.dcnm.carapp.core.vehicle.source

import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 디버그 전용 시나리오 재생기. 에뮬레이터가 못 주는 신호(차속)를 재생할 때만 쓴다.
 *
 * 여기서 나가는 샘플은 전부 [SignalSource.SIMULATED]이며 UI에 SIMULATED 배지를 띄운다.
 * 실측 신호와 같은 종류를 동시에 내보내지 않는다 (시연 신뢰성).
 */
class MockSignalSource : VehicleSignalSource {

    private val _signals = MutableSharedFlow<VehicleSignal>(extraBufferCapacity = 64)
    override val signals: Flow<VehicleSignal> = _signals.asSharedFlow()

    override suspend fun start(): Boolean = true
    override suspend fun stop() = Unit

    /** 시연 스크립트에서 호출. 예: 차속 0→40km/h 램프. */
    fun emitSpeed(kmh: Float, nowMillis: Long = System.currentTimeMillis()) {
        _signals.tryEmit(VehicleSignal.Speed(kmh, nowMillis, SignalSource.SIMULATED))
    }
}
