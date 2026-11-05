package com.dcnm.carapp.core.vehicle.source

import android.content.Context
import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Pleos Vehicle SDK(ai.pleos.playground:Vehicle) 어댑터.
 *
 * 구독 대상 (전부 읽기):
 * - Brake.registerBrakePedalStatus            -> VehicleSignal.BrakePedal   (CAR_DRIVE)
 * - SteeringWheel.registerSteeringWheelAngle   -> VehicleSignal.SteeringAngle (READ_CAR_STEERING)
 * - Gear.registerGearSelection                 -> VehicleSignal.Gear         (CAR_DRIVE)  ※ 실패 시 AAOS 폴백
 * - TurnSignal.register*                       -> VehicleSignal.TurnSignal   (CAR_LIGHTS)
 * - Odometer.get*                              -> VehicleSignal.Odometer     (CAR_MILEAGE)
 * - Tire.getLowTirePressure                    -> VehicleSignal.LowTirePressure (CAR_TIRES)
 * - Safety.registerTelltales                   -> VehicleSignal.Telltale     (CAR_SAFETY, PRIVATE 가능성 → 선택 입력)
 *
 * 1주차 스파이크에서 각 API의 Capability·권한·갱신 주기를 확인한 뒤 채운다.
 * SDK 호출은 모두 IPC 비동기(onComplete/onFailed)이며 initialize()/release()로 연결을 연다.
 */
class PleosVehicleSignalSource(
    private val context: Context,
) : VehicleSignalSource {

    private val _signals = MutableSharedFlow<VehicleSignal>(extraBufferCapacity = 256)
    override val signals: Flow<VehicleSignal> = _signals.asSharedFlow()

    // TODO(spike-2): private val vehicle = ai.pleos.playground.vehicle.Vehicle(context.applicationContext)

    override suspend fun start(): Boolean {
        // TODO(spike-2): vehicle.initialize() → check*Capabilities → register* 리스너 등록.
        //  리스너 콜백에서 _signals.tryEmit(...) 로 변환. 권한 오류(ERROR_SECURITY_PERMISSION)는 false 반환.
        return false
    }

    override suspend fun stop() {
        // TODO(spike-2): unregister* → vehicle.release()
    }
}
