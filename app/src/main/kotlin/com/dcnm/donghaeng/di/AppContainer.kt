package com.dcnm.donghaeng.di

import android.content.Context
import com.dcnm.donghaeng.BuildConfig
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.LiveKitCallClient
import com.dcnm.donghaeng.core.location.LocationSource
import com.dcnm.donghaeng.core.location.PleosLocationSource
import com.dcnm.donghaeng.core.location.SimulatedLocationSource
import com.dcnm.donghaeng.core.navi.NaviController
import com.dcnm.donghaeng.core.navi.PleosNaviController
import com.dcnm.donghaeng.core.network.FakeTripRepository
import com.dcnm.donghaeng.core.network.RemoteTripRepository
import com.dcnm.donghaeng.core.network.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 수동 DI. 앱 전체에서 하나씩만 만든다. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val trips: TripRepository =
        if (BuildConfig.API_BASE_URL.isBlank()) FakeTripRepository(appScope)
        else RemoteTripRepository(BuildConfig.API_BASE_URL, appScope, debugLogging = BuildConfig.DEBUG)

    val call: CallClient = LiveKitCallClient(context, appScope)

    val location: LocationSource =
        if (BuildConfig.SIMULATE_LOCATION) SimulatedLocationSource() else PleosLocationSource(context)

    val navi: NaviController = PleosNaviController(context)
}
