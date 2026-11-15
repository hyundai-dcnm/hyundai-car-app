package com.dcnm.donghaeng.di

import android.content.Context
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.LiveKitCallClient
import com.dcnm.donghaeng.core.location.LocationSource
import com.dcnm.donghaeng.vehicle.ServerTripRepository
import com.dcnm.donghaeng.core.navi.NaviSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 수동 DI. 앱 전체에서 하나씩만 만든다. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val trips = ServerTripRepository(appScope)

    val navi = NaviSource(context)

    val call: CallClient = LiveKitCallClient(context, appScope)

    /** 차량 위치(Pleos FusedLocation). 권한을 받은 뒤 MainActivity 에서 start() 한다 */
    val location: LocationSource = LocationSource(context).apply { initialize() }
}
