package com.dcnm.carapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 차량 화면: 다크 배경, 큰 글자, 색만으로 상태를 전달하지 않는다. */
private val CarColors = darkColorScheme()

private val CarTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp),
    headlineMedium = TextStyle(fontSize = 32.sp),
    bodyLarge = TextStyle(fontSize = 24.sp),
)

@Composable
fun CarAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarColors, typography = CarTypography, content = content)
}
