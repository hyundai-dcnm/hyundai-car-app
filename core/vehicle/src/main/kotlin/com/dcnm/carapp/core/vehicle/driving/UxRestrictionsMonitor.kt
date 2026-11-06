package com.dcnm.carapp.core.vehicle.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * CarUxRestrictionsManager 구독. 주행/정차 판정의 1순위 입력이다.
 *
 * - requiresDistractionOptimization == true  → 주행 중 규제 (음성 전용 화면)
 * - false                                    → 정차
 * - 자동차 기기가 아니거나 연결 실패        → null (신호 기반 폴백)
 *
 * 에뮬레이터 검증: `scripts/driving-state.sh driving | parked`
 */
class UxRestrictionsMonitor(
    private val context: Context,
    private val onChanged: (restricted: Boolean?) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            onChanged(null)
            return
        }
        runCatching {
            val c = Car.createCar(context.applicationContext) ?: error("Car.createCar returned null")
            car = c
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            manager = m
            m.registerListener { onChanged(it.restricted()) }
            onChanged(m.currentCarUxRestrictions?.restricted())
        }.onFailure {
            Log.w(TAG, "CarUxRestrictions 연결 실패, 신호 기반 판정으로 대체", it)
            onChanged(null)
        }
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private fun CarUxRestrictions.restricted(): Boolean = isRequiresDistractionOptimization

    private companion object {
        const val TAG = "UxRestrictions"
    }
}
