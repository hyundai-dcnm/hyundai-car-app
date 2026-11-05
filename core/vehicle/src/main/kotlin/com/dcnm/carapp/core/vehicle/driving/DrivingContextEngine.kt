package com.dcnm.carapp.core.vehicle.driving

import com.dcnm.carapp.core.model.DrivingState
import com.dcnm.carapp.core.model.GearPosition
import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 주행/정차 판정. 입력은 두 가지다.
 *
 * 1. CarUxRestrictionsManager 콜백 (있으면 우선) — 에뮬레이터에서는
 *    `adb shell cmd hmg_car_service --service drivingstate driving` 으로 토글해 검증한다.
 * 2. 신호 기반 폴백: 기어 P 이고 차속 <= 5km/h 이면 PARKED, 그 외 DRIVING.
 *
 * 결과는 UI 모드(주행 중 음성 전용 / 정차 중 리포트·QR)와 STT 허용 여부를 결정한다.
 */
class DrivingContextEngine {

    private val _state = MutableStateFlow(DrivingState.UNKNOWN)
    val state: StateFlow<DrivingState> = _state

    private var lastGear: GearPosition = GearPosition.UNKNOWN
    private var lastSpeedKmh: Float = 0f
    private var uxRestricted: Boolean? = null

    /** CarUxRestrictions 콜백에서 호출. null이면 신호 기반 폴백으로 돌아간다. */
    fun onUxRestrictionsChanged(restricted: Boolean?) {
        uxRestricted = restricted
        recompute()
    }

    fun onSignal(signal: VehicleSignal) {
        when (signal) {
            is VehicleSignal.Gear -> lastGear = signal.gear
            is VehicleSignal.Speed -> lastSpeedKmh = signal.kmh
            else -> return
        }
        recompute()
    }

    private fun recompute() {
        val next = when (uxRestricted) {
            true -> DrivingState.DRIVING
            false -> DrivingState.PARKED
            null -> when {
                lastGear == GearPosition.UNKNOWN -> DrivingState.UNKNOWN
                lastGear == GearPosition.P && lastSpeedKmh <= 5f -> DrivingState.PARKED
                else -> DrivingState.DRIVING
            }
        }
        _state.value = next
    }
}
