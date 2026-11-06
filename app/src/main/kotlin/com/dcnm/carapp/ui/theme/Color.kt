package com.dcnm.carapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pleos Connect UI Design Guide 원값 (foundations/colors 표 이미지 판독, 2026-11-06).
 * 근거: reports/차안부 차량 UI 분석.md
 */
internal object Pleos {
    // Light
    val LightBackground = Color(0xFFF7F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFEDEEF2)
    val LightOutline = Color(0xFFE0E1E6)
    val LightPositive = Color(0xFF0064FF)
    val LightNegative = Color(0xFFFE3D16)

    // Dark
    val Basic00 = Color(0xFF131417)
    val Basic50 = Color(0xFF1D1E21)
    val Basic100 = Color(0xFF313236)
    val Basic200 = Color(0xFF44464E)
    val Basic300 = Color(0xFF686A72)
    val Basic400 = Color(0xFFA4A8AE)
    val DarkPositive = Color(0xFF0082FF)
    val DarkNegative = Color(0xFFFF4C28)
}

/**
 * 앱 의미색. Material ColorScheme에 없는 역할만 여기 둔다.
 *
 * 규칙
 * - 누를 수 있는 채움은 [action] 하나뿐 (순정 PRND 선택 박스와 같은 검정 반전).
 * - 기능색(파랑·빨강)은 선·아이콘 전용. 글자는 [ink]/[inkSecondary]/[inkTertiary]/[errorText]만.
 * - Pleos regulation 색(노랑·주황·빨강 원값)과 순정 활성 주황(#FF9900)은 쓰지 않는다.
 * - Pleos 팔레트 밖 색은 발화 앰버 계열(voice*) 하나뿐.
 */
@Immutable
data class CarColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    /** 카드 구분선. 경계로만 쓰고 컨트롤 테두리로 쓰지 않는다 (대비 1.31). */
    val divider: Color,
    /** 비선택 컨트롤 테두리 2dp (비텍스트 대비 5.39). */
    val controlBorder: Color,
    val ink: Color,
    /** 84% 알파는 Pleos 원값 미확인, 제안값. */
    val inkSecondary: Color,
    /** 26sp 이상에만. */
    val inkTertiary: Color,
    val action: Color,
    val onAction: Color,
    /** 링크·2차 버튼 글자 (라이트 한정). */
    val link: Color,
    /** 그래프 신호선. 다크 #0082FF는 글자 대비 미달이라 선 전용. */
    val signalLine: Color,
    val voiceTint: Color,
    val voiceInk: Color,
    val errorText: Color,
    val errorIcon: Color,
)

val LightCarColors = CarColors(
    background = Pleos.LightBackground,
    surface = Pleos.LightSurface,
    surfaceVariant = Pleos.LightSurfaceVariant,
    divider = Pleos.LightOutline,
    controlBorder = Pleos.Basic300,
    ink = Pleos.Basic00,
    inkSecondary = Color(0xD6131417),
    inkTertiary = Pleos.Basic300,
    action = Pleos.Basic00,
    onAction = Color.White,
    link = Pleos.LightPositive,
    signalLine = Pleos.LightPositive,
    voiceTint = Color(0xFFFFF1DA),
    voiceInk = Color(0xFF8A4B00),
    errorText = Color(0xFFC42E0F),
    errorIcon = Pleos.LightNegative,
)

val DarkCarColors = CarColors(
    background = Pleos.Basic00,
    surface = Pleos.Basic50,
    surfaceVariant = Pleos.Basic100,
    divider = Pleos.Basic200,
    controlBorder = Pleos.Basic400,
    ink = Color.White,
    inkSecondary = Color(0xD6FFFFFF),
    inkTertiary = Pleos.Basic400,
    action = Color.White,
    onAction = Pleos.Basic00,
    link = Color.White,
    signalLine = Pleos.DarkPositive,
    voiceTint = Color(0xFF3A2C14),
    voiceInk = Color(0xFFF0B35A),
    errorText = Pleos.DarkNegative,
    errorIcon = Pleos.DarkNegative,
)

val LocalCarColors = staticCompositionLocalOf { LightCarColors }
