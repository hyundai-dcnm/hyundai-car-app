package com.dcnm.carapp.ui.parked

import com.dcnm.carapp.core.model.SignalSample
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.core.model.SymptomTrigger
import java.util.UUID
import kotlin.math.sin

/**
 * 디버그 빌드 시연용 샘플 기록 (가상 시나리오). 모든 샘플은 [SignalSource.SIMULATED]라
 * 화면에 "합성 신호 포함" 배지가 붙는다. 실측과 섞지 않는다.
 *
 * 구성: 말하기 26초 전부터만 확보(앞 4초 공백), 3초 전 브레이크 82%, 차속 42→18km/h, 기어 D, 적산 48,213km.
 */
fun demoSymptomEvent(now: Long = System.currentTimeMillis()): SymptomEvent {
    val anchor = now - 10 * 60_000L
    val samples = buildList {
        for (i in -26..10) {
            val t = anchor + i * 1_000L
            val speed = when {
                i < -4 -> 38f + (i + 26) * 0.2f
                i < 2 -> 42f - (i + 4) * 4f
                else -> 18f
            }
            add(SignalSample(t, "speed", "%.1f".format(speed), "km/h", SignalSource.SIMULATED))
            val brake = when (i) { -4 -> 30; -3 -> 82; -2 -> 64; -1 -> 40; 0 -> 15; else -> 0 }
            add(SignalSample(t, "brake", brake.toString(), "%", SignalSource.SIMULATED))
            if (i % 10 == 0) {
                add(SignalSample(t, "gear", "D", "", SignalSource.SIMULATED))
                add(SignalSample(t, "odometer", "%.1f".format(48_213.4 + (i + 26) * 0.01), "km", SignalSource.SIMULATED))
            }
            add(SignalSample(t, "steering", "%.1f".format(4f * sin(i / 3.0).toFloat()), "deg", SignalSource.SIMULATED))
        }
    }
    return SymptomEvent(
        id = UUID.randomUUID().toString(),
        createdAtMillis = anchor,
        trigger = SymptomTrigger.USER_TAP,
        utterance = "방금 브레이크 밟을 때 덜컹했어",
        snapshot = SignalSnapshot(
            anchorMillis = anchor,
            requestedBeforeMillis = 30_000,
            requestedAfterMillis = 10_000,
            coveredMillis = 36_000,
            samples = samples,
        ),
    )
}
