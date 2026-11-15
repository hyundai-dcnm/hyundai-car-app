package com.dcnm.donghaeng.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow

/** Only platform UX restrictions may unlock configuration. No speed==0 inference. */
class VehicleUxMonitor(private val context: Context) {
    val mode = MutableStateFlow(UxMode.Unknown)
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    fun start() {
        mode.value = UxMode.Unknown
        runCatching {
            car = Car.createCar(context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (!ready) { mode.value = UxMode.Unknown; manager = null }
                else runCatching {
                    manager = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                    val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { r ->
                        mode.value = if (!r.isRequiresDistractionOptimization && r.activeRestrictions == 0) UxMode.Unrestricted else UxMode.Restricted
                    }
                    manager?.registerListener(listener)
                    manager?.currentCarUxRestrictions?.let(listener::onUxRestrictionsChanged)
                }.onFailure { mode.value = UxMode.Unknown }
            }
        }.onFailure { mode.value = UxMode.Unknown }
    }
    fun stop() { mode.value = UxMode.Unknown; runCatching { manager?.unregisterListener(); car?.disconnect() }; manager = null; car = null }
}
