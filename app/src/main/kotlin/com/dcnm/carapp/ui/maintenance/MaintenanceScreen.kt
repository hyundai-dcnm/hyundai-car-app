package com.dcnm.carapp.ui.maintenance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.di.AppContainer

@Composable
fun MaintenanceScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: MaintenanceViewModel = viewModel { MaintenanceViewModel(container.maintenanceRepository) }
    val records by vm.records.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("정비 결과·교체 이력", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(records, key = { it.id }) { r ->
                Text("${r.item} · ${r.workType} · ${r.odometerKmAtWork ?: "-"}km", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        // TODO(W3): 입력 폼 (작업 종류·항목·작업일·당시 주행거리). 정차 중이므로 키보드 허용.
        Button(onClick = onBack) { Text("뒤로") }
    }
}
