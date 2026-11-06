package com.dcnm.carapp.ui.theme

import androidx.compose.ui.unit.dp

/** 앱 창 1682×1210dp(가로) 기준 치수. 1dp≈0.147mm. */
object CarDimens {
    // 그리드: 12열, 바깥 여백 48, 거터 24
    val Margin = 48.dp
    val Gutter = 24.dp
    /** 이보다 좁으면(분할 창) 1열로 접는다. */
    val TwoColumnMinWidth = 1000.dp

    // 터치
    val DriverTouch = 100.dp
    val SecondaryTouch = 80.dp
    val DriverSpacing = 32.dp
    val Spacing = 24.dp

    // 버튼 (Pleos L/M)
    val ButtonL = 100.dp
    val RadiusL = 32.dp
    val ButtonM = 80.dp
    val RadiusM = 24.dp

    // 주행 화면
    val DrivingButtonWidth = 1000.dp
    val DrivingButtonHeight = 400.dp
    val RecordingBorder = 8.dp

    // 카드
    val CardRadius = 24.dp
    val CardPadding = 32.dp
    val ControlBorder = 2.dp
    /** 카드 안 줄과 줄 사이. 순정 설정 화면의 제목→설명 간격 실측값(16px @160dpi). */
    val LineGap = 16.dp

    const val PressedScale = 0.95f
}
