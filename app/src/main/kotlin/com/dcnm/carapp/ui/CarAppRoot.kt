package com.dcnm.carapp.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dcnm.carapp.core.vehicle.driving.ScreenMode
import com.dcnm.carapp.core.vehicle.driving.initialScreenMode
import com.dcnm.carapp.core.vehicle.driving.toScreenMode
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.driving.DrivingScreen
import com.dcnm.carapp.ui.maintenance.MaintenanceScreen
import com.dcnm.carapp.ui.parked.ParkedScreen
import com.dcnm.carapp.ui.records.RecordsScreen
import com.dcnm.carapp.ui.report.MechanicQrScreen
import com.dcnm.carapp.ui.report.ReportScreen

object Routes {
    const val PARKED = "parked"
    const val RECORDS = "records"
    const val MAINTENANCE = "maintenance"
    const val REPORT = "report/{id}"
    const val QR = "qr/{id}"
    fun report(id: String) = "report/$id"
    fun qr(id: String) = "qr/$id"
}

/**
 * 주행 상태에 따라 화면을 강제 분기한다. 전환 규칙은 [toScreenMode] 참고.
 * - DRIVING: 음성 전용 화면 (대형 버튼 1개 + 상태 1줄).
 * - PARKED : 홈(최근 분석) → 기록 목록 → 기록 분석 → 정비사용 QR, 정비 결과 기록. 주행 전 보던 정차 화면 스택을 그대로 복원한다.
 * - PARKED_NO_SIGNAL: 차량 신호가 없는 기기(폰 에뮬레이터 등). 정차 화면 + 신호 없음 표시.
 *
 * 전환 애니메이션은 넣지 않는다 (Pleos 주의분산 가이드: 자동 애니메이션 최소화).
 */
@Composable
fun CarAppRoot(container: AppContainer) {
    val drivingState = container.vehicleRepository.drivingState
    val screenModes = remember(drivingState) { drivingState.toScreenMode() }
    val mode by screenModes.collectAsState(initial = initialScreenMode(drivingState.value))
    // 주행 화면으로 갔다 와도 정차 화면 스택이 유지되도록 NavHost 밖에서 만든다.
    val parkedNav = rememberNavController()

    when (mode) {
        ScreenMode.DRIVING -> DrivingScreen(container)
        ScreenMode.PARKED, ScreenMode.PARKED_NO_SIGNAL ->
            ParkedNavHost(container, parkedNav, noVehicleSignal = mode == ScreenMode.PARKED_NO_SIGNAL)
    }
}

@Composable
private fun ParkedNavHost(container: AppContainer, nav: NavHostController, noVehicleSignal: Boolean) {
    NavHost(
        navController = nav,
        startDestination = Routes.PARKED,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable(Routes.PARKED) {
            ParkedScreen(
                container,
                noVehicleSignal = noVehicleSignal,
                onOpenReport = { id -> nav.navigate(Routes.report(id)) },
                onOpenRecords = { nav.navigate(Routes.RECORDS) },
                onOpenMaintenance = { nav.navigate(Routes.MAINTENANCE) },
            )
        }
        composable(Routes.RECORDS) {
            RecordsScreen(container, onBack = { nav.popBackStack() }, onOpenReport = { id -> nav.navigate(Routes.report(id)) })
        }
        composable(Routes.REPORT) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ReportScreen(container, eventId = id, onBack = { nav.popBackStack() }, onMakeQr = { nav.navigate(Routes.qr(id)) })
        }
        composable(Routes.QR) { entry ->
            MechanicQrScreen(container, eventId = entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.MAINTENANCE) { MaintenanceScreen(container, onBack = { nav.popBackStack() }) }
    }
}
