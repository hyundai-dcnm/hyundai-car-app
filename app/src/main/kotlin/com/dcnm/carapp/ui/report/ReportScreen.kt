package com.dcnm.carapp.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.ActionButton
import com.dcnm.carapp.ui.components.CarCard
import com.dcnm.carapp.ui.components.MetaLine
import com.dcnm.carapp.ui.components.ScreenHeader
import com.dcnm.carapp.ui.components.SectionLabel
import com.dcnm.carapp.ui.components.SimulatedBadge
import com.dcnm.carapp.ui.components.VoiceBlock
import com.dcnm.carapp.ui.format.dateTime
import com.dcnm.carapp.ui.format.label
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/**
 * 기록 분석 대시보드. 위에서 아래로:
 * 요약 3줄 → (좌) 말씀하신 내용·관측 요약·기록 상태·정비사용 QR 만들기 / (우) 한 시간축 그래프·관련 이력.
 * 가로 창에서는 스크롤 없이 한 화면, 분할 창(폭 < 1000dp)에서만 1열 + 스크롤.
 */
@Composable
fun ReportScreen(container: AppContainer, eventId: String, onBack: () -> Unit, onMakeQr: () -> Unit) {
    val vm: ReportViewModel = viewModel(key = eventId) {
        ReportViewModel(eventId, container.symptomRepository, container.maintenanceRepository)
    }
    val state by vm.state.collectAsState()
    val event = state.event
    val c = CarTheme.colors

    Column(Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 24.dp)) {
        ScreenHeader("기록 분석", onBack = onBack) {
            if (event != null) SimulatedBadge(sourcesOf(event.snapshot))
        }
        if (event == null) {
            Text("기록 없음", style = CarTheme.type.body, color = c.inkSecondary)
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = CarDimens.Spacing)) {
            val wide = maxWidth >= CarDimens.TwoColumnMinWidth
            Column(
                modifier = if (wide) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing),
            ) {
                SummaryBox(event)
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CarDimens.Gutter)) {
                        LeftColumn(event, onMakeQr, Modifier.weight(1f))
                        RightColumn(state, Modifier.weight(1f))
                    }
                } else {
                    LeftColumn(event, onMakeQr, Modifier.fillMaxWidth())
                    RightColumn(state, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(event: SymptomEvent) {
    val c = CarTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surfaceVariant, RoundedCornerShape(CarDimens.CardRadius)).padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(CarDimens.LineGap),
    ) {
        MetaLine(dateTime(event.createdAtMillis), event.trigger.label(), style = CarTheme.type.keyFigure, color = c.ink)
        Text("${coverageLine(event.snapshot)}, 녹음 30초 전부터 10초 후까지", style = CarTheme.type.body, color = c.ink)
        Text("녹음 내용과 측정값 구분 표시, 고장 판정 아님", style = CarTheme.type.body, color = c.inkSecondary)
    }
}

@Composable
private fun LeftColumn(event: SymptomEvent, onMakeQr: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        VoiceBlock(event.utterance)
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("측정값")
            observations(event.snapshot).forEach { Text(it, style = CarTheme.type.body, color = CarTheme.colors.ink) }
        }
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("기록 상태")
            val sources = sourcesOf(event.snapshot).joinToString { it.label() }.ifEmpty { "없음" }
            Text("출처: $sources", style = CarTheme.type.body, color = CarTheme.colors.ink)
            Text("상태: ${event.status.label()}", style = CarTheme.type.body, color = CarTheme.colors.ink)
        }
        ActionButton("정비사용 QR 만들기", onMakeQr, Modifier.fillMaxWidth())
    }
}

@Composable
private fun RightColumn(state: ReportViewModel.UiState, modifier: Modifier) {
    val event = state.event ?: return
    val c = CarTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("녹음 시점 전후 차량 신호")
            SignalChart(buildLanes(event.snapshot), Modifier.fillMaxWidth())
        }
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("관련 이력")
            Text(
                if (state.sameTriggerCount > 0) "같은 방식 기록 ${state.sameTriggerCount}건 더 있음" else "같은 방식 기록 없음",
                style = CarTheme.type.body, color = c.ink,
            )
            Text(latestMaintenanceLine(state.latestMaintenance), style = CarTheme.type.body, color = c.ink)
        }
    }
}

private fun latestMaintenanceLine(r: MaintenanceRecord?): String =
    if (r == null) "정비 기록 없음" else "최근 정비: ${r.item} ${r.workType.label()}, ${dateTime(r.workDateMillis, withTime = false)}"
