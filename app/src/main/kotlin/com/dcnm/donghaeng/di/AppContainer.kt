package com.dcnm.donghaeng.di

import android.content.Context
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.LiveKitCallClient
import com.dcnm.donghaeng.core.location.LocationSource
import com.dcnm.donghaeng.vehicle.ServerTripRepository
import com.dcnm.donghaeng.core.navi.DestinationAutoRouter
import com.dcnm.donghaeng.core.navi.NaviSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map

/** 수동 DI. 앱 전체에서 하나씩만 만든다. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val trips = ServerTripRepository(appScope)

    /** 차량 내비(Pleos NaviHelper). start()/release() 는 MainActivity 에서 한다 */
    val navi = NaviSource(context)

    val call: CallClient = LiveKitCallClient(context, appScope)

    /** 차량 위치(Pleos FusedLocation). 권한을 받은 뒤 MainActivity 에서 start() 한다 */
    val location: LocationSource = LocationSource(context).apply { initialize() }

    init {
        // 여행방 목적지가 확정되면 내 차 내비에 길 안내를 요청한다
        DestinationAutoRouter(navi, appScope).bind(trips.trip.map { it?.destination })
    }
}
