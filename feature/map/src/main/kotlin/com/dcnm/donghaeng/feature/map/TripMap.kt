package com.dcnm.donghaeng.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng

data class MapPin(val id: String, val label: String, val location: LatLng, val isMe: Boolean)

/**
 * 일행 위치 지도. 담당: ①
 *
 * TODO(①): 지도 SDK(NAVER 지도 후보)로 교체. 지금은 좌표를 화면 비율로 찍기만 한다.
 *  바깥에서 쓰는 모양(pins, destination)은 유지하면 화면 쪽은 고칠 게 없다.
 */
@Composable
fun TripMap(
    pins: List<MapPin>,
    destination: Destination?,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 18.sp, color = Color(0xFF1B1D21))
    Box(modifier.background(Color(0xFFE9EDF2))) {
        Canvas(Modifier.matchParentSize().padding(48.dp)) {
            val points = pins.map { it.location } + listOfNotNull(destination?.location)
            if (points.isEmpty()) return@Canvas
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }
            fun project(p: LatLng): Offset {
                val x = if (maxLng == minLng) 0.5 else (p.lng - minLng) / (maxLng - minLng)
                val y = if (maxLat == minLat) 0.5 else 1 - (p.lat - minLat) / (maxLat - minLat)
                return Offset((x * size.width).toFloat(), (y * size.height).toFloat())
            }
            destination?.let {
                val at = project(it.location)
                drawCircle(Color(0xFFE5484D), radius = 14.dp.toPx(), center = at)
                drawText(measurer, it.name, at + Offset(20.dp.toPx(), -12.dp.toPx()), labelStyle)
            }
            pins.forEach { pin ->
                val at = project(pin.location)
                drawCircle(if (pin.isMe) Color(0xFF1F6FEB) else Color(0xFF2DA44E), radius = 12.dp.toPx(), center = at)
                drawText(measurer, pin.label, at + Offset(18.dp.toPx(), -12.dp.toPx()), labelStyle)
            }
        }
        if (pins.isEmpty()) {
            Text(
                "공유 중인 위치가 없어요",
                Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
