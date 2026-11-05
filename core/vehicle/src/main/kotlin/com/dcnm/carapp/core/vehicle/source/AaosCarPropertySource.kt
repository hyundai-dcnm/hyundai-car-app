package com.dcnm.carapp.core.vehicle.source

import android.content.Context
import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AAOS CarPropertyManager 폴백. Pleos SDK에 없거나 막힌 표준 속성을 읽는다.
 *
 * - PERF_VEHICLE_SPEED (0x11600207) -> VehicleSignal.Speed  ※ 에뮬레이터 Virtual Console에 차속 카드가 없음
 * - GEAR_SELECTION                  -> VehicleSignal.Gear   (CAR_POWERTRAIN, normal)
 *
 * 공식 정책이 "표준 데이터(속도·기어)는 AAOS Car API 권장"이므로 기어는 이쪽을 1순위로 둘 수도 있다.
 * 1주차 스파이크 3번 항목에서 결정한다.
 */
class AaosCarPropertySource(
    private val context: Context,
) : VehicleSignalSource {

    private val _signals = MutableSharedFlow<VehicleSignal>(extraBufferCapacity = 256)
    override val signals: Flow<VehicleSignal> = _signals.asSharedFlow()

    override suspend fun start(): Boolean {
        // TODO(spike-1,3): android.car.Car.createCar(context) → CarPropertyManager
        //  registerCallback(PERF_VEHICLE_SPEED, GEAR_SELECTION). 차량 권한은 android.car.permission.CAR_SPEED / CAR_POWERTRAIN.
        return false
    }

    override suspend fun stop() {
        // TODO: unregisterCallback, car.disconnect()
    }
}
