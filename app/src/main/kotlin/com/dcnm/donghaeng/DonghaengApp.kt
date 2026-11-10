package com.dcnm.donghaeng

import android.app.Application
import com.dcnm.donghaeng.di.AppContainer

class DonghaengApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
