package com.dcnm.carapp.ui.report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.CarCard
import com.dcnm.carapp.ui.components.ScreenHeader
import com.dcnm.carapp.ui.components.SectionLabel
import com.dcnm.carapp.ui.components.SimulatedBadge
import com.dcnm.carapp.ui.format.dateTime
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/**
 * 정비사에게 보여줄 QR. 휴대폰 카메라로 찍으면 web/mechanic/ 페이지가 열린다.
 * 좌: QR(다크 모드에서도 흰 바탕 유지 — 카메라 인식) / 우: 무엇이 담기는지와 담기지 않는지.
 */
@Composable
fun MechanicQrScreen(container: AppContainer, eventId: String, onBack: () -> Unit) {
    val vm: ReportViewModel = viewModel(key = eventId) {
        ReportViewModel(eventId, container.symptomRepository, container.maintenanceRepository)
    }
    val state by vm.state.collectAsState()
    val event = state.event
    val c = CarTheme.colors

    Column(Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 24.dp)) {
        ScreenHeader("정비사용 QR", onBack = onBack) {
            if (event != null) SimulatedBadge(sourcesOf(event.snapshot))
        }
        val url = vm.mechanicUrl(state)
        if (event == null || url == null) {
            Text("기록 없음", style = CarTheme.type.body, color = c.inkSecondary)
            return@Column
        }
        val qr = remember(url) { qrBitmap(url, QR_PX) }
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = CarDimens.Spacing)) {
            val wide = maxWidth >= CarDimens.TwoColumnMinWidth
            val qrSize: Dp = if (wide) min(maxHeight, 640.dp) else min(maxWidth, 560.dp)
            val qrBlock: @Composable () -> Unit = {
                Image(
                    qr, contentDescription = "정비 참고 리포트 QR",
                    modifier = Modifier
                        .size(qrSize)
                        .background(Color.White, RoundedCornerShape(CarDimens.CardRadius))
                        .padding(24.dp),
                )
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.Top) {
                    qrBlock()
                    Guide(event.createdAtMillis, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
                    qrBlock()
                    Guide(event.createdAtMillis, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Guide(createdAtMillis: Long, modifier: Modifier) {
    val c = CarTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        Text("정비사에게 이 화면 제시", style = CarTheme.type.title, color = c.ink)
        Text(
            "휴대폰 카메라로 촬영 시 ${dateTime(createdAtMillis)} 기록의 정비 참고 리포트 열림. 별도 앱 설치 불필요.",
            style = CarTheme.type.body, color = c.ink,
        )
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("포함 항목")
            Text("녹음 내용", style = CarTheme.type.body, color = c.ink)
            Text("녹음 30초 전부터 10초 후까지 차속, 브레이크, 조향", style = CarTheme.type.body, color = c.ink)
            Text("같은 방식 기록 횟수, 최근 정비 1건", style = CarTheme.type.body, color = c.ink)
        }
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("제외 항목")
            Text("이름, 연락처, 위치 정보 제외. 내용은 QR 안에만 있으며 서버에 저장되지 않음.", style = CarTheme.type.body, color = c.ink)
        }
    }
}

private const val QR_PX = 720
