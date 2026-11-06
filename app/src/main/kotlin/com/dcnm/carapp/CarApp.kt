package com.dcnm.carapp

import android.app.Application
import com.dcnm.carapp.di.AppContainer
import kotlinx.coroutines.launch

class CarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 주행/정차 판정은 화면과 무관하게 앱 생명주기 동안 유지한다.
        container.uxRestrictionsMonitor.start()
        container.appScope.launch { container.vehicleRepository.start() }
    }
}
