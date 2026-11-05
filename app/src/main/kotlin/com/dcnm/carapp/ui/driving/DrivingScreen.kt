package com.dcnm.carapp.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.di.AppContainer

/**
 * 주행 중 화면. DO 규정: 대형 버튼 1개 + 상태 1줄. 키보드·목록·차트 없음.
 */
@Composable
fun DrivingScreen(container: AppContainer) {
    val vm: DrivingViewModel = viewModel {
        DrivingViewModel(container.vehicleRepository, container.voiceManager, container.symptomRepository, container.naviRepository)
    }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = vm::onRecordTapped, modifier = Modifier.height(160.dp)) {
            Text("상황 기록", style = MaterialTheme.typography.displayLarge)
        }
        Text(
            text = when (val s = state) {
                DrivingViewModel.UiState.Idle -> "준비됨"
                DrivingViewModel.UiState.Listening -> "듣고 있어요"
                is DrivingViewModel.UiState.Recorded -> "기록됨"
                is DrivingViewModel.UiState.AskingWaypoint -> "${s.shopName} 경유?"
                is DrivingViewModel.UiState.Message -> s.text
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
