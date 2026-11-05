package com.dcnm.carapp.ui.parked

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
fun ParkedScreen(container: AppContainer, onOpenMaintenance: () -> Unit) {
    val vm: ParkedViewModel = viewModel { ParkedViewModel(container.symptomRepository) }
    val events by vm.events.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("기록된 증상", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(events, key = { it.id }) { e ->
                // TODO(W3): 리포트 카드 — 발화 원문, 30초 그래프, 확보 길이(coveredMillis), QR
                Text(
                    text = "${e.utterance ?: "(발화 없음)"} · 확보 ${e.snapshot.coveredMillis / 1000}s · ${e.status}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        Button(onClick = onOpenMaintenance) { Text("정비 결과·교체 이력") }
    }
}
