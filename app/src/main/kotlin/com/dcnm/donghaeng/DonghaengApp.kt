package com.dcnm.donghaeng

import android.app.Application
import com.dcnm.donghaeng.call.CallService
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.di.AppContainer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class DonghaengApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 통화 연결을 시작하는 순간(사용자가 버튼을 눌러 앱이 앞에 있을 때) 서비스를 띄운다. 끝나면 서비스가 스스로 멈춘다.
        MainScope().launch {
            container.call.state
                .distinctUntilChangedBy { it is CallState.Connecting }
                .collect { if (it is CallState.Connecting) CallService.start(this@DonghaengApp) }
        }
    }
}
