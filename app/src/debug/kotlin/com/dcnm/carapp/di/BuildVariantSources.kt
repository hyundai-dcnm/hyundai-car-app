package com.dcnm.carapp.di

import com.dcnm.carapp.core.vehicle.source.MockSignalSource
import com.dcnm.carapp.core.vehicle.source.VehicleSignalSource

/** 디버그: 시연용 Mock 재생기를 추가한다. 숨김 메뉴에서 [mock]을 조작한다. */
object BuildVariantSources {
    val mock = MockSignalSource()
    fun extraSources(): List<VehicleSignalSource> = listOf(mock)
}
