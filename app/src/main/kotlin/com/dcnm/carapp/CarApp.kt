package com.dcnm.carapp

import android.app.Application
import com.dcnm.carapp.di.AppContainer

class CarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
