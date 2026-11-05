package com.dcnm.carapp.core.model

/**
 * 차량에서 수신한 단일 신호 샘플.
 *
 * 모든 신호는 수신 시각(epochMillis)과 출처(source)를 함께 남긴다.
 * 보간값·합성값은 [SignalSource.SIMULATED]로 표시하고 실측과 섞지 않는다.
 */
sealed interface VehicleSignal {
    val epochMillis: Long
    val source: SignalSource

    data class Speed(val kmh: Float, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class Gear(val gear: GearPosition, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class BrakePedal(val percent: Int, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class SteeringAngle(val degrees: Float, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class TurnSignal(val state: TurnSignalState, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class Odometer(val km: Double, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class Telltale(val id: String, val on: Boolean, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
    data class LowTirePressure(val on: Boolean, override val epochMillis: Long, override val source: SignalSource) : VehicleSignal
}

enum class GearPosition { P, R, N, D, UNKNOWN }

enum class TurnSignalState { OFF, LEFT, RIGHT, HAZARD }

/** 신호 출처. 리포트에는 출처를 그대로 표기한다. */
enum class SignalSource {
    /** Pleos Vehicle SDK */
    PLEOS_SDK,
    /** AAOS CarPropertyManager (차속·기어 폴백) */
    AAOS_CAR_API,
    /** 디버그 전용 재생 데이터. 화면에 SIMULATED 표기 필수 */
    SIMULATED,
}
