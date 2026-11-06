package com.dcnm.carapp.ui.parked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.BuildConfig
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.ActionButton
import com.dcnm.carapp.ui.components.CarCard
import com.dcnm.carapp.ui.components.MetaLine
import com.dcnm.carapp.ui.components.SectionLabel
import com.dcnm.carapp.ui.components.SimulatedBadge
import com.dcnm.carapp.ui.components.StatusChip
import com.dcnm.carapp.ui.components.VoiceBlock
import com.dcnm.carapp.ui.format.dateTime
import com.dcnm.carapp.ui.format.km
import com.dcnm.carapp.ui.format.label
import com.dcnm.carapp.ui.report.coverageLine
import com.dcnm.carapp.ui.report.label
import com.dcnm.carapp.ui.report.observations
import com.dcnm.carapp.ui.report.sourcesOf
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/**
 * 정차 홈. 좌: 최근 차량 분석(가장 최근 기록 1건) / 우: 기록 목록·최근 정비·수집 상태. 폭 < 1000dp면 1열.
 * 평소에는 순정 내비 화면이 앞에 있고, 이 화면은 운전자가 앱을 열었을 때만 본다.
 */
@Composable
fun ParkedScreen(
    container: AppContainer,
    noVehicleSignal: Boolean,
    onOpenReport: (String) -> Unit,
    onOpenRecords: () -> Unit,
    onOpenMaintenance: () -> Unit,
) {
    val vm: ParkedViewModel = viewModel { ParkedViewModel(container.symptomRepository, container.maintenanceRepository) }
    val events by vm.events.collectAsState()
    val latest by vm.latestMaintenance.collectAsState()
    val c = CarTheme.colors

    BoxWithConstraints(Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 32.dp)) {
        val wide = maxWidth >= CarDimens.TwoColumnMinWidth
        val left: @Composable (Modifier) -> Unit = { m -> LatestAnalysis(events.firstOrNull(), onOpenReport, m) }
        val right: @Composable (Modifier) -> Unit = { m ->
            SideColumn(events.size, latest, noVehicleSignal, onOpenRecords, onOpenMaintenance, onAddDemo = vm::addDemoEvent, modifier = m)
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(CarDimens.Gutter)) {
                left(Modifier.weight(1f))
                right(Modifier.weight(1f))
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
                left(Modifier.fillMaxWidth())
                right(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LatestAnalysis(event: SymptomEvent?, onOpen: (String) -> Unit, modifier: Modifier) {
    val c = CarTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        Text("최근 차량 분석", style = CarTheme.type.title, color = c.ink)
        if (event == null) {
            CarCard(Modifier.fillMaxWidth()) {
                Text("기록 없음", style = CarTheme.type.bodyStrong, color = c.ink)
                Text("주행 중 음성으로 증상을 말하면 직전 30초 차량 신호와 함께 저장", style = CarTheme.type.body, color = c.inkSecondary)
            }
            return@Column
        }
        CarCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaLine(dateTime(event.createdAtMillis), event.trigger.label(), modifier = Modifier.weight(1f))
                SimulatedBadge(sourcesOf(event.snapshot))
                StatusChip(event.status.label())
            }
            VoiceBlock(event.utterance)
            SectionLabel("측정값")
            observations(event.snapshot).take(3).forEach { Text(it, style = CarTheme.type.body, color = c.ink) }
            Text(coverageLine(event.snapshot), style = CarTheme.type.meta, color = c.inkSecondary)
        }
        ActionButton("분석 보기", { onOpen(event.id) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SideColumn(
    recordCount: Int,
    latest: MaintenanceRecord?,
    noVehicleSignal: Boolean,
    onOpenRecords: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onAddDemo: () -> Unit,
    modifier: Modifier,
) {
    val c = CarTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        Text(" ", style = CarTheme.type.title) // 좌측 제목과 카드 윗선 맞춤
        ActionButton("기록 목록 보기 (${recordCount}건)", onOpenRecords, Modifier.fillMaxWidth(), filled = false)
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("최근 정비")
            if (latest == null) {
                Text("정비 기록 없음", style = CarTheme.type.bodyStrong, color = c.ink)
                Text("정비 결과 입력 시 다음 점검 기준 함께 생성", style = CarTheme.type.body, color = c.inkSecondary)
            } else {
                Text("${latest.item} ${latest.workType.label()}", style = CarTheme.type.keyFigure, color = c.ink)
                Text(
                    listOfNotNull(dateTime(latest.workDateMillis, withTime = false), latest.odometerKmAtWork?.let(::km)).joinToString(", "),
                    style = CarTheme.type.body, color = c.inkSecondary,
                )
            }
        }
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("차량 신호")
            if (noVehicleSignal) {
                StatusChip("차량 신호 없음", warning = true)
                Text("차량 기기에서 실행 시 기록 가능", style = CarTheme.type.body, color = c.inkSecondary)
            } else {
                Text("주행 상태 연결됨", style = CarTheme.type.bodyStrong, color = c.ink)
                Text("내비 화면에서도 음성 기록 가능, 직전 30초 차량 신호 포함", style = CarTheme.type.body, color = c.inkSecondary)
            }
        }
        ActionButton("정비 결과 기록", onOpenMaintenance, Modifier.fillMaxWidth(), filled = false)
        if (BuildConfig.DEBUG) ActionButton("시연용 샘플 기록 추가 (합성)", onAddDemo, Modifier.fillMaxWidth(), filled = false, height = CarDimens.ButtonM)
    }
}
