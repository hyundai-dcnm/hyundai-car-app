package com.dcnm.carapp.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme

/**
 * 누를 수 있는 모든 요소의 바탕. 눌림은 95% 축소(애니메이션 없이 값만 바뀜) + 햅틱.
 * Pleos 가이드: 입력 반응 0.25초 이내, 터치 시 촉각 피드백.
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val view = LocalView.current
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (pressed) CarDimens.PressedScale else 1f
                scaleX = s; scaleY = s
            }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** 채움 버튼은 검정 반전 한 종류뿐 (순정 PRND 선택 박스 문법). */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    height: Dp = CarDimens.ButtonL,
) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(if (height >= CarDimens.ButtonL) CarDimens.RadiusL else CarDimens.RadiusM)
    val bg = if (filled && enabled) c.action else c.surface
    val fg = when {
        !enabled -> c.inkTertiary
        filled -> c.onAction
        else -> c.ink
    }
    Pressable(
        onClick = onClick,
        shape = shape,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = height)
            .background(bg, shape)
            .then(if (filled && enabled) Modifier else Modifier.border(CarDimens.ControlBorder, c.controlBorder, shape)),
    ) {
        Text(text, style = CarTheme.type.bodyStrong, color = fg, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

/** 선택형 컨트롤(세그먼트·칩). 선택 = 검정 반전, 비선택 = 흰 면 + 2dp 테두리. */
@Composable
fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = CarDimens.SecondaryTouch,
) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(CarDimens.RadiusM)
    Pressable(
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .heightIn(min = height)
            .background(if (selected) c.action else c.surface, shape)
            .then(if (selected) Modifier else Modifier.border(CarDimens.ControlBorder, c.controlBorder, shape)),
    ) {
        Text(
            text,
            style = if (selected) CarTheme.type.bodyStrong else CarTheme.type.body,
            color = if (selected) c.onAction else c.ink,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
fun CarCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = CarTheme.colors
    Column(
        modifier = modifier
            .background(c.surface, RoundedCornerShape(CarDimens.CardRadius))
            .border(1.dp, c.divider, RoundedCornerShape(CarDimens.CardRadius))
            .padding(CarDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(CarDimens.LineGap),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = CarTheme.type.meta, color = CarTheme.colors.inkSecondary, modifier = modifier)
}

/** 상태 칩. 색이 아니라 테두리와 글자로 상태를 전달한다. [warning]이면 ⚠ 기호를 붙인다. */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = if (warning) "⚠ $text" else text,
        style = CarTheme.type.meta,
        color = if (warning) c.errorText else c.ink,
        modifier = modifier
            .border(CarDimens.ControlBorder, if (warning) c.errorIcon else c.controlBorder, shape)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/**
 * 운전자 발화 블록. 앱에서 유일한 팔레트 밖 색(앰버 틴트 면).
 * 앰버는 면·라벨로만 쓰고 밑줄·테두리로 쓰지 않는다 (순정 활성 주황과 혼동 방지).
 */
@Composable
fun VoiceBlock(utterance: String?, modifier: Modifier = Modifier, label: String = "녹음 내용") {
    val c = CarTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.voiceTint, RoundedCornerShape(CarDimens.RadiusM))
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("“ $label", style = CarTheme.type.meta, color = c.voiceInk)
        Text(
            text = utterance ?: "녹음 내용 없음",
            style = CarTheme.type.body,
            color = if (utterance != null) c.ink else c.inkTertiary,
        )
    }
}

/** 합성 신호가 섞여 있으면 반드시 표시한다. 시연 신뢰성의 기본. */
@Composable
fun SimulatedBadge(sources: Collection<SignalSource>, modifier: Modifier = Modifier) {
    if (SignalSource.SIMULATED !in sources) return
    StatusChip("합성 신호 포함", modifier = modifier, warning = true)
}

/** 날짜와 기록 방식처럼 짧은 정보를 한 줄에 나란히 둔다. 구분 기호 없이 간격으로만 나눈다. */
@Composable
fun MetaLine(vararg parts: String, modifier: Modifier = Modifier, style: TextStyle = CarTheme.type.meta, color: Color = CarTheme.colors.inkSecondary) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        parts.forEach { Text(it, style = style, color = color) }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = CarDimens.ButtonL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CarDimens.Spacing),
    ) {
        if (onBack != null) ActionButton("‹ 뒤로", onBack, filled = false)
        Text(title, style = CarTheme.type.title, color = CarTheme.colors.ink, modifier = Modifier.weight(1f))
        trailing()
    }
}
