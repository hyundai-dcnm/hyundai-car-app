package com.dcnm.carapp.core.vehicle.buffer

import com.dcnm.carapp.core.model.SignalSample
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.VehicleSignal
import java.util.ArrayDeque

/**
 * 메모리 순환 버퍼. 수신한 신호를 [retentionMillis] 동안 보관하고, 사건 발생 시 구간을 잘라 [SignalSnapshot]으로 만든다.
 *
 * - SDK가 과거 로그를 돌려주지 않으므로 앱이 켜진 뒤 받은 샘플만 있다.
 * - 확보한 길이가 요청보다 짧으면 [SignalSnapshot.coveredMillis]에 그대로 드러낸다. 과거를 복원하거나 보간하지 않는다.
 * - 스레드 안전: 단일 코루틴에서 append/cut 하도록 상위([VehicleRepository])가 보장한다.
 */
class SignalRingBuffer(
    private val retentionMillis: Long = 45_000,
) {
    private val deque = ArrayDeque<VehicleSignal>()

    fun append(signal: VehicleSignal) {
        deque.addLast(signal)
        val cutoff = signal.epochMillis - retentionMillis
        while (deque.isNotEmpty() && deque.first.epochMillis < cutoff) deque.removeFirst()
    }

    /**
     * [anchorMillis] 기준 이전 [beforeMillis], 이후 [afterMillis] 구간을 잘라낸다.
     * 이후 구간은 호출 시점까지 들어온 샘플만 포함한다 (후속 샘플을 더 담으려면 지연 호출).
     */
    fun cut(anchorMillis: Long, beforeMillis: Long = 30_000, afterMillis: Long = 5_000): SignalSnapshot {
        val from = anchorMillis - beforeMillis
        val to = anchorMillis + afterMillis
        val samples = deque.filter { it.epochMillis in from..to }
        val covered = if (samples.isEmpty()) 0 else samples.last().epochMillis - samples.first().epochMillis
        return SignalSnapshot(
            anchorMillis = anchorMillis,
            requestedBeforeMillis = beforeMillis,
            requestedAfterMillis = afterMillis,
            coveredMillis = covered,
            samples = samples.map { it.toSample() },
        )
    }

    fun size(): Int = deque.size
}

internal fun VehicleSignal.toSample(): SignalSample = when (this) {
    is VehicleSignal.Speed -> SignalSample(epochMillis, "speed", kmh.toString(), "km/h", source)
    is VehicleSignal.Gear -> SignalSample(epochMillis, "gear", gear.name, "", source)
    is VehicleSignal.BrakePedal -> SignalSample(epochMillis, "brake", percent.toString(), "%", source)
    is VehicleSignal.SteeringAngle -> SignalSample(epochMillis, "steering", degrees.toString(), "deg", source)
    is VehicleSignal.TurnSignal -> SignalSample(epochMillis, "turn_signal", state.name, "", source)
    is VehicleSignal.Odometer -> SignalSample(epochMillis, "odometer", km.toString(), "km", source)
    is VehicleSignal.Telltale -> SignalSample(epochMillis, "telltale:$id", on.toString(), "", source)
    is VehicleSignal.LowTirePressure -> SignalSample(epochMillis, "low_tire_pressure", on.toString(), "", source)
}
