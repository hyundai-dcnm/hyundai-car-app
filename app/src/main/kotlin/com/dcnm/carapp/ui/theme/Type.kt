package com.dcnm.carapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 시스템 sans-serif를 그대로 쓴다. Pleos 기기는 이것이 Asta Sans(42dot)이고
 * 굵기 매칭(500=Medium, 700·800=ExtraBold)도 순정 화면과 같아진다. 폰트 파일은 번들하지 않는다.
 */
private val SystemSans = FontFamily.SansSerif

/**
 * 줄간격·글자간격·줄넘김은 순정 화면 실측값에 맞춘다 (Pleos 에뮬레이터 v2.1.0, 2026-11-06, 차량 설정·헤드업 알림).
 * - 줄간격: 글꼴 기본 높이 그대로 (24sp 본문 줄 간격 29px = 1.21배). 추가 행간 없음.
 * - 글자간격: 0 (렌더 폭이 Asta Sans 무자간 폭과 1px 이내로 일치).
 * - 줄넘김: 글자 단위 (한국어 어절 중간에서도 넘김). TextView 기본값과 같은 HighQuality·Default.
 * 줄과 줄 사이 여백은 행간 대신 [CarDimens.LineGap]으로 띄운다.
 */
private val NativeLineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Default,
    wordBreak = LineBreak.WordBreak.Default,
)

private fun style(size: TextUnit, weight: Int) = TextStyle(
    fontFamily = SystemSans,
    fontSize = size,
    fontWeight = FontWeight(weight),
    letterSpacing = 0.sp,
    lineBreak = NativeLineBreak,
)

/**
 * 차량 화면 글자 단계. 17" 160dpi에서 1sp≈0.147mm → 본문 30sp≈4.4mm (Pleos 핵심 4mm 기준 충족).
 * 20sp 이하는 쓰지 않는다. [axis]는 리포트 그래프 축·QR 캡션 전용.
 */
@Immutable
data class CarType(
    val drivingAction: TextStyle = style(60.sp, 800),
    val drivingStatus: TextStyle = style(40.sp, 700),
    val keyFigure: TextStyle = style(40.sp, 700),
    val title: TextStyle = style(36.sp, 700),
    val body: TextStyle = style(30.sp, 500),
    val bodyStrong: TextStyle = style(30.sp, 700),
    val meta: TextStyle = style(26.sp, 500),
    val axis: TextStyle = style(24.sp, 500),
)

val LocalCarType = staticCompositionLocalOf { CarType() }
