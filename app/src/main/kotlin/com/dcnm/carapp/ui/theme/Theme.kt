package com.dcnm.carapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Pleos 라이트 네이티브 테마. 시스템 day/night를 따른다 (Pleos 가이드: 주야 자동 대응).
 * 앱 화면은 [CarTheme.colors]·[CarTheme.type]을 쓰고, MaterialTheme은 기본 컴포넌트용으로만 맞춰 둔다.
 */
@Composable
fun CarAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkCarColors else LightCarColors
    val material = if (darkTheme) {
        darkColorScheme(
            primary = colors.action, onPrimary = colors.onAction,
            background = colors.background, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.inkSecondary,
            outline = colors.controlBorder, error = colors.errorText,
        )
    } else {
        lightColorScheme(
            primary = colors.action, onPrimary = colors.onAction,
            background = colors.background, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.inkSecondary,
            outline = colors.controlBorder, error = colors.errorText,
        )
    }
    CompositionLocalProvider(LocalCarColors provides colors, LocalCarType provides CarType()) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

object CarTheme {
    val colors: CarColors
        @Composable @ReadOnlyComposable get() = LocalCarColors.current
    val type: CarType
        @Composable @ReadOnlyComposable get() = LocalCarType.current
}
