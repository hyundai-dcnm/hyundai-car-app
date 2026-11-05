package com.dcnm.carapp.core.vehicle

import com.dcnm.carapp.core.model.DrivingState
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.SymptomTrigger
import com.dcnm.carapp.core.model.VehicleSignal
import com.dcnm.carapp.core.vehicle.buffer.SignalRingBuffer
import com.dcnm.carapp.core.vehicle.driving.DrivingContextEngine
import com.dcnm.carapp.core.vehicle.source.VehicleSignalSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 차량 신호의 단일 진입점. 신호 소스 → 링버퍼 → 주행 판정 → 트리거 감지까지 한 코루틴에서 처리한다.
 *
 * 외부에는 세 가지만 노출한다:
 * - [drivingState]: 주행/정차
 * - [triggers]: 앱이 먼저 물어야 할 순간 (급제동·큰 조향·경고등)
 * - [snapshot]: 사건 시각 기준 구간 잘라내기
 */
class VehicleRepository(
    private val source: VehicleSignalSource,
    private val scope: CoroutineScope,
    private val buffer: SignalRingBuffer = SignalRingBuffer(),
    private val drivingEngine: DrivingContextEngine = DrivingContextEngine(),
    private val triggerDetector: TriggerDetector = TriggerDetector(),
) {
    val drivingState: StateFlow<DrivingState> = drivingEngine.state

    private val _triggers = MutableSharedFlow<SymptomTrigger>(extraBufferCapacity = 8)
    val triggers: SharedFlow<SymptomTrigger> = _triggers.asSharedFlow()

    private var collectJob: Job? = null

    suspend fun start(): Boolean {
        val ok = source.start()
        collectJob = scope.launch {
            source.signals.collect { signal ->
                buffer.append(signal)
                drivingEngine.onSignal(signal)
                triggerDetector.onSignal(signal)?.let { _triggers.tryEmit(it) }
            }
        }
        return ok
    }

    suspend fun stop() {
        collectJob?.cancel()
        source.stop()
    }

    fun snapshot(anchorMillis: Long = System.currentTimeMillis()): SignalSnapshot = buffer.cut(anchorMillis)

    fun onUxRestrictionsChanged(restricted: Boolean?) = drivingEngine.onUxRestrictionsChanged(restricted)

    val rawSignals: Flow<VehicleSignal> get() = source.signals
}

/**
 * "앱이 먼저 묻기" 트리거. 임계값은 1주차 스파이크 결과로 조정한다.
 * - 브레이크 >= 70% 급상승 → HARD_BRAKE
 * - 조향각 변화 >= 60deg/1s → SHARP_STEERING
 * - 경고등 OFF→ON → TELLTALE_ON
 */
class TriggerDetector(
    private val hardBrakePercent: Int = 70,
    private val sharpSteeringDegrees: Float = 60f,
) {
    private var lastBrake = 0
    private var lastSteering: Pair<Long, Float>? = null
    private val telltaleOn = mutableSetOf<String>()

    fun onSignal(signal: VehicleSignal): SymptomTrigger? = when (signal) {
        is VehicleSignal.BrakePedal -> {
            val fired = lastBrake < hardBrakePercent && signal.percent >= hardBrakePercent
            lastBrake = signal.percent
            if (fired) SymptomTrigger.HARD_BRAKE else null
        }
        is VehicleSignal.SteeringAngle -> {
            val prev = lastSteering
            lastSteering = signal.epochMillis to signal.degrees
            if (prev != null && signal.epochMillis - prev.first <= 1_000 &&
                kotlin.math.abs(signal.degrees - prev.second) >= sharpSteeringDegrees
            ) SymptomTrigger.SHARP_STEERING else null
        }
        is VehicleSignal.Telltale -> {
            val fired = signal.on && telltaleOn.add(signal.id)
            if (!signal.on) telltaleOn.remove(signal.id)
            if (fired) SymptomTrigger.TELLTALE_ON else null
        }
        else -> null
    }
}
