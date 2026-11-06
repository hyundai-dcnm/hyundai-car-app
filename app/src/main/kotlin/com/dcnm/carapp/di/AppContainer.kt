package com.dcnm.carapp.di

import android.content.Context
import com.dcnm.carapp.capture.SymptomCapture
import com.dcnm.carapp.core.data.db.AppDatabase
import com.dcnm.carapp.core.data.repository.MaintenanceRepository
import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.navi.BundledShopProvider
import com.dcnm.carapp.core.navi.NaviRepository
import com.dcnm.carapp.core.navi.PleosNaviRepository
import com.dcnm.carapp.core.vehicle.VehicleRepository
import com.dcnm.carapp.core.vehicle.driving.UxRestrictionsMonitor
import com.dcnm.carapp.core.vehicle.source.AaosCarPropertySource
import com.dcnm.carapp.core.vehicle.source.CompositeVehicleSignalSource
import com.dcnm.carapp.core.vehicle.source.PleosVehicleSignalSource
import com.dcnm.carapp.core.vehicle.source.VehicleSignalSource
import com.dcnm.carapp.core.voice.PleosVoiceManager
import com.dcnm.carapp.core.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 수동 DI 컨테이너. 앱 생명주기 동안 하나만 존재한다.
 * 디버그 빌드는 [BuildVariantSources.extraSources]로 MockSignalSource를 끼워 넣는다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db: AppDatabase by lazy { AppDatabase.build(appContext) }

    val symptomRepository: SymptomRepository by lazy { SymptomRepository(db.symptomEventDao()) }
    val maintenanceRepository: MaintenanceRepository by lazy { MaintenanceRepository(db.maintenanceRecordDao()) }

    val vehicleRepository: VehicleRepository by lazy {
        val sources: List<VehicleSignalSource> = buildList {
            add(PleosVehicleSignalSource(appContext))
            add(AaosCarPropertySource(appContext))
            addAll(BuildVariantSources.extraSources())
        }
        VehicleRepository(CompositeVehicleSignalSource(sources), appScope)
    }

    /** 주행/정차 1순위 입력. 콜백은 [VehicleRepository.onUxRestrictionsChanged]로 넘긴다. */
    val uxRestrictionsMonitor: UxRestrictionsMonitor by lazy {
        UxRestrictionsMonitor(appContext) { restricted -> vehicleRepository.onUxRestrictionsChanged(restricted) }
    }

    val voiceManager: VoiceManager by lazy { PleosVoiceManager(appContext) }

    val naviRepository: NaviRepository by lazy { PleosNaviRepository(appContext, BundledShopProvider(appContext)) }

    /** 기록 루프. 주행 화면과 백그라운드 서비스가 같은 인스턴스를 쓴다. */
    val symptomCapture: SymptomCapture by lazy {
        SymptomCapture(vehicleRepository, voiceManager, symptomRepository, naviRepository, appScope)
    }
}
