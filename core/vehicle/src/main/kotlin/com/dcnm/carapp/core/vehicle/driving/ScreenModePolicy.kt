package com.dcnm.carapp.core.vehicle.driving

import com.dcnm.carapp.core.model.DrivingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/** 화면이 실제로 보여줄 모드. [DrivingState]를 그대로 쓰지 않고 전환 규칙을 거친다. */
enum class ScreenMode { DRIVING, PARKED, PARKED_NO_SIGNAL }

/**
 * 주행/정차 화면 전환 규칙.
 *
 * - 주행 진입: 즉시.
 * - 정차 복귀: PARKED가 [parkDelayMillis] 동안 유지돼야 전환 (P단 순간 통과로 깜빡이지 않게).
 * - 판정 불가(UNKNOWN): 안전 측으로 주행 화면. [unknownTimeoutMillis] 지나도 그대로면
 *   차량 신호가 없는 기기로 보고 정차 화면 + "차량 신호 없음" 표시.
 * - 첫 상태가 PARKED면 지연 없이 정차 화면 (앱 시작 시 주행 화면이 1초 비치는 것 방지).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<DrivingState>.toScreenMode(
    parkDelayMillis: Long = 1_000,
    unknownTimeoutMillis: Long = 3_000,
): Flow<ScreenMode> = flow {
    var first = true
    emitAll(
        transformLatest { state ->
            val settle = !first
            first = false
            when (state) {
                DrivingState.DRIVING -> emit(ScreenMode.DRIVING)
                DrivingState.PARKED -> {
                    if (settle) delay(parkDelayMillis)
                    emit(ScreenMode.PARKED)
                }
                DrivingState.UNKNOWN -> {
                    emit(ScreenMode.DRIVING)
                    delay(unknownTimeoutMillis)
                    emit(ScreenMode.PARKED_NO_SIGNAL)
                }
            }
        },
    )
}.distinctUntilChanged()

/** 첫 프레임에 쓸 모드. 수집이 시작되기 전 한 프레임도 규칙과 어긋나지 않게 한다. */
fun initialScreenMode(current: DrivingState): ScreenMode =
    if (current == DrivingState.PARKED) ScreenMode.PARKED else ScreenMode.DRIVING
