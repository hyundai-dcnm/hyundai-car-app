package com.dcnm.carapp.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.CarCard
import com.dcnm.carapp.ui.components.MetaLine
import com.dcnm.carapp.ui.components.Pressable
import com.dcnm.carapp.ui.components.ScreenHeader
import com.dcnm.carapp.ui.components.SimulatedBadge
import com.dcnm.carapp.ui.components.StatusChip
import com.dcnm.carapp.ui.format.dateTime
import com.dcnm.carapp.ui.parked.ParkedViewModel
import com.dcnm.carapp.ui.report.coverageLine
import com.dcnm.carapp.ui.report.label
import com.dcnm.carapp.ui.report.sourcesOf
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/** 기록 목록. 최신순. 누르면 기록 분석으로 간다. */
@Composable
fun RecordsScreen(container: AppContainer, onBack: () -> Unit, onOpenReport: (String) -> Unit) {
    val vm: ParkedViewModel = viewModel { ParkedViewModel(container.symptomRepository, container.maintenanceRepository) }
    val events by vm.events.collectAsState()
    val c = CarTheme.colors

    Column(
        Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing),
    ) {
        ScreenHeader("기록 목록", onBack = onBack) {
            Text("${events.size}건", style = CarTheme.type.meta, color = c.inkSecondary)
        }
        if (events.isEmpty()) {
            CarCard(Modifier.fillMaxWidth()) {
                Text("기록 없음", style = CarTheme.type.bodyStrong, color = c.ink)
                Text("주행 중 음성으로 증상을 말하면 직전 30초 차량 신호와 함께 저장", style = CarTheme.type.body, color = c.inkSecondary)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
            items(events, key = { it.id }) { e -> EventCard(e, onClick = { onOpenReport(e.id) }) }
        }
    }
}

@Composable
private fun EventCard(e: SymptomEvent, onClick: () -> Unit) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(CarDimens.CardRadius)
    Pressable(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth().background(c.surface, shape)) {
        Column(Modifier.fillMaxWidth().padding(CarDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(CarDimens.LineGap)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaLine(dateTime(e.createdAtMillis), e.trigger.label(), modifier = Modifier.weight(1f))
                StatusChip(e.status.label())
            }
            Row(
                Modifier.fillMaxWidth().background(c.voiceTint, RoundedCornerShape(CarDimens.RadiusM)).padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    "“ ${e.utterance ?: "녹음 내용 없음"}",
                    style = CarTheme.type.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(coverageLine(e.snapshot), style = CarTheme.type.meta, color = c.inkSecondary, modifier = Modifier.weight(1f))
                SimulatedBadge(sourcesOf(e.snapshot))
                Text("분석 보기 ›", style = CarTheme.type.bodyStrong, color = c.ink)
            }
        }
    }
}
