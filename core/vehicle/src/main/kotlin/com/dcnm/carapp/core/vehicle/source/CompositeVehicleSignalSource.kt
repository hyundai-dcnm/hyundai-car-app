package com.dcnm.carapp.core.vehicle.source

import com.dcnm.carapp.core.model.VehicleSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * 여러 소스를 하나의 Flow로 합친다. 같은 종류 신호가 두 소스에서 오면 [SignalSource] 우선순위로 상위에서 걸러낸다.
 *
 * 기본 조합: Pleos SDK + AAOS 폴백 (+ 디버그 빌드에서 MockSignalSource).
 */
class CompositeVehicleSignalSource(
    private val sources: List<VehicleSignalSource>,
) : VehicleSignalSource {

    override val signals: Flow<VehicleSignal> = merge(*sources.map { it.signals }.toTypedArray())

    /** 하나라도 성공하면 true. 전부 실패하면 상위에서 "신호 없음" 상태를 표시한다. */
    override suspend fun start(): Boolean = sources.map { it.start() }.any { it }

    override suspend fun stop() = sources.forEach { it.stop() }
}
