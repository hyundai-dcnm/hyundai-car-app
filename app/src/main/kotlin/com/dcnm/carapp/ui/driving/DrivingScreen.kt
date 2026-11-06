package com.dcnm.carapp.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.capture.SymptomCapture
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.Pressable
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/**
 * 주행 중 화면. 대형 버튼 1개(경유 질문일 때만 예/아니요 2개) + 상태 1줄 + 정적 안내 1줄.
 * 키보드·목록·차트·움직이는 요소 없음. 상태는 버튼 모양 자체로도 보여준다(버튼만 보는 운전자 대비).
 */
@Composable
fun DrivingScreen(container: AppContainer) {
    val vm: DrivingViewModel = viewModel {
        DrivingViewModel(container.symptomCapture)
    }
    val state by vm.state.collectAsState()
    DrivingContent(state, onRecord = { vm.onRecordTapped() }, onWaypointAnswer = vm::onWaypointAnswer)
}

@Composable
fun DrivingContent(
    state: SymptomCapture.State,
    onRecord: () -> Unit,
    onWaypointAnswer: (Boolean) -> Unit,
) {
    val c = CarTheme.colors
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 40.dp),
    ) {
        // 분할 창(폭 < 1000dp)에서는 창 폭의 90%로 줄인다. 높이는 유지.
        val buttonWidth: Dp = if (maxWidth < CarDimens.TwoColumnMinWidth) maxWidth * 0.9f else CarDimens.DrivingButtonWidth

        Text(
            text = statusLine(state),
            style = CarTheme.type.drivingStatus,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Box(Modifier.align(Alignment.Center)) {
            when (state) {
                is SymptomCapture.State.AskingWaypoint -> Row(horizontalArrangement = Arrangement.spacedBy(CarDimens.DriverSpacing)) {
                    val half = (buttonWidth - CarDimens.DriverSpacing) / 2
                    DrivingButton("예", filled = true, width = half, onClick = { onWaypointAnswer(true) })
                    DrivingButton("아니요", filled = false, width = half, onClick = { onWaypointAnswer(false) })
                }
                SymptomCapture.State.Listening -> DrivingButton("● 녹음 중", filled = false, width = buttonWidth, onClick = null)
                is SymptomCapture.State.Recorded -> DrivingButton("✓ 기록 완료", filled = true, width = buttonWidth, onClick = null)
                SymptomCapture.State.Idle -> DrivingButton("상황 기록", filled = true, width = buttonWidth, onClick = onRecord)
            }
        }

        Text(
            text = "기록 확인은 정차(P) 후 가능",
            style = CarTheme.type.body,
            color = c.inkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
}

private fun statusLine(state: SymptomCapture.State): String = when (state) {
    SymptomCapture.State.Idle -> "이상 증상 발생 시 버튼 누른 뒤 음성으로 기록"
    SymptomCapture.State.Listening -> "녹음 중"
    is SymptomCapture.State.Recorded -> "녹음 내용과 차량 신호 저장 완료"
    is SymptomCapture.State.AskingWaypoint -> "${state.shopName} 경유지 추가"
}

/** 주행 화면 전용 대형 버튼. 채움 = 검정 반전, 비채움 = 흰 면 + 8dp 검정 테두리(녹음 중·아니요). */
@Composable
private fun DrivingButton(text: String, filled: Boolean, width: Dp, onClick: (() -> Unit)?) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(CarDimens.RadiusL)
    Pressable(
        onClick = onClick ?: {},
        shape = shape,
        enabled = onClick != null,
        modifier = Modifier
            .width(width)
            .height(CarDimens.DrivingButtonHeight)
            .background(if (filled) c.action else c.surface, shape)
            .then(if (filled) Modifier else Modifier.border(CarDimens.RecordingBorder, c.action, shape)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = CarTheme.type.drivingAction, color = if (filled) c.onAction else c.ink)
        }
    }
}
