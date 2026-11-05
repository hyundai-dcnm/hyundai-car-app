package com.dcnm.carapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dcnm.carapp.core.model.DrivingState
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.driving.DrivingScreen
import com.dcnm.carapp.ui.maintenance.MaintenanceScreen
import com.dcnm.carapp.ui.parked.ParkedScreen

object Routes {
    const val DRIVING = "driving"
    const val PARKED = "parked"
    const val MAINTENANCE = "maintenance"
}

/**
 * 주행 상태에 따라 화면을 강제 분기한다.
 * - DRIVING: 음성 전용 화면 (대형 버튼 1개 + 상태 1줄)
 * - PARKED : 리포트·QR·교체 기록
 */
@Composable
fun CarAppRoot(container: AppContainer) {
    val nav = rememberNavController()
    val drivingState by container.vehicleRepository.drivingState.collectAsState()

    LaunchedEffect(drivingState) {
        when (drivingState) {
            DrivingState.DRIVING -> nav.navigate(Routes.DRIVING) { popUpTo(0) }
            DrivingState.PARKED -> nav.navigate(Routes.PARKED) { popUpTo(0) }
            DrivingState.UNKNOWN -> Unit
        }
    }

    NavHost(navController = nav, startDestination = Routes.PARKED) {
        composable(Routes.DRIVING) { DrivingScreen(container) }
        composable(Routes.PARKED) { ParkedScreen(container, onOpenMaintenance = { nav.navigate(Routes.MAINTENANCE) }) }
        composable(Routes.MAINTENANCE) { MaintenanceScreen(container, onBack = { nav.popBackStack() }) }
    }
}
