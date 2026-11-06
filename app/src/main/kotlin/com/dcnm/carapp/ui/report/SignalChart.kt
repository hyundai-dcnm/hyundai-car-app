package com.dcnm.carapp.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dcnm.carapp.ui.theme.CarTheme

private const val FROM_S = -30f
private const val TO_S = 10f
private val LabelWidth = 150.dp

/**
 * 발화 시점(0초)을 기준으로 -30s~+10s 한 시간축에 신호 레인을 쌓는다.
 * - 신호선은 Pleos 블루 한 색. 레인은 이름 + 선 모양(실선·계단선·점선)으로 구분.
 * - 발화 시점은 앰버 세로선 + "말한 시점" 글자.
 * - 신호 공백은 빗금 + "없음". 보간하지 않는다.
 * 정적 그림이며 애니메이션이 없다.
 */
@Composable
fun SignalChart(lanes: List<Lane>, modifier: Modifier = Modifier, laneHeight: Dp = 120.dp) {
    val c = CarTheme.colors
    val t = CarTheme.type
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 발화 시점 라벨
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val plotWidth = maxWidth - LabelWidth
            val x = LabelWidth + plotWidth * ((0f - FROM_S) / (TO_S - FROM_S))
            Text("녹음 시점", style = t.axis, color = c.voiceInk, modifier = Modifier.offset(x = x - 60.dp))
        }
        lanes.forEach { lane ->
            Row(Modifier.fillMaxWidth().height(laneHeight), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(LabelWidth)) {
                    Text(lane.label, style = t.meta, color = c.ink)
                    Text("${lane.unit}, ${lane.style.label()}", style = t.axis, color = c.inkTertiary)
                }
                LanePlot(lane, Modifier.weight(1f).fillMaxHeight())
            }
        }
        // x축 눈금
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(LabelWidth))
            BoxWithConstraints(Modifier.weight(1f).height(36.dp)) {
                listOf(-30, -20, -10, 0, 10).forEach { s ->
                    val x = maxWidth * ((s - FROM_S) / (TO_S - FROM_S))
                    Text(if (s > 0) "+${s}초" else "${s}초", style = t.axis, color = c.inkTertiary, modifier = Modifier.offset(x = x - 28.dp))
                }
            }
        }
    }
}

private fun LaneStyle.label() = when (this) {
    LaneStyle.SOLID -> "실선"
    LaneStyle.STEP -> "계단선"
    LaneStyle.DASHED -> "점선"
}

@Composable
private fun LanePlot(lane: Lane, modifier: Modifier) {
    val c = CarTheme.colors
    val gapRanges = gaps(lane, FROM_S, TO_S)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            fun xOf(s: Float) = size.width * ((s - FROM_S) / (TO_S - FROM_S))
            fun yOf(v: Float) = size.height - size.height * (v / lane.maxValue).coerceIn(0f, 1f)

            // 바닥선
            drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            // 공백 빗금
            gapRanges.forEach { r ->
                val left = xOf(r.start); val right = xOf(r.endInclusive)
                clipRect(left, 0f, right, size.height) {
                    var x = left - size.height
                    while (x < right) {
                        drawLine(c.controlBorder, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 2f)
                        x += 18f
                    }
                }
            }
            // 신호선
            val pts = lane.points.sortedBy { it.seconds }.filter { it.seconds in FROM_S..TO_S }
            if (pts.isNotEmpty()) {
                val path = Path()
                pts.forEachIndexed { i, p ->
                    val x = xOf(p.seconds); val y = yOf(p.value)
                    when {
                        i == 0 -> path.moveTo(x, y)
                        lane.style == LaneStyle.STEP -> { path.lineTo(x, yOf(pts[i - 1].value)); path.lineTo(x, y) }
                        else -> path.lineTo(x, y)
                    }
                }
                drawPath(
                    path, c.signalLine,
                    style = Stroke(
                        width = 5f,
                        pathEffect = if (lane.style == LaneStyle.DASHED) PathEffect.dashPathEffect(floatArrayOf(18f, 12f)) else null,
                    ),
                )
            }
            // 발화 시점
            val x0 = xOf(0f)
            drawLine(c.voiceInk, Offset(x0, 0f), Offset(x0, size.height), strokeWidth = 4f)
        }
        if (lane.points.isEmpty()) {
            Text("없음", style = CarTheme.type.meta, color = c.ink, modifier = Modifier.align(Alignment.Center))
        }
    }
}
