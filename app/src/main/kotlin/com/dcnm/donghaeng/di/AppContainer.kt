package com.dcnm.donghaeng.di

import android.content.Context
import com.dcnm.donghaeng.BuildConfig
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.LiveKitCallClient
import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.trip.FakeTripRepository
import com.dcnm.donghaeng.core.trip.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 수동 DI. 앱 전체에서 하나씩만 만든다. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val devCall = CallCredentials(BuildConfig.DEV_LIVEKIT_URL, BuildConfig.DEV_LIVEKIT_TOKEN)
        .takeIf { it.url.isNotBlank() && it.token.isNotBlank() }

    // TODO: ④와 API 를 정하면 서버 연동 구현으로 바꾼다.
    val trips: TripRepository = FakeTripRepository(appScope, devCall)

    val call: CallClient = LiveKitCallClient(context, appScope)
}
