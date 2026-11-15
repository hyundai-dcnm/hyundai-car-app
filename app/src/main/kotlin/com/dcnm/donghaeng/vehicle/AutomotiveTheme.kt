package com.dcnm.donghaeng.vehicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caverock.androidsvg.SVG

object AutoColors {
    val Ink = Color(0xFF181E28)
    val Panel = Color(0xFF202733)
    val Raised = Color(0xFF2B3442)
    val Line = Color(0xFF3B4656)
    val Text = Color(0xFFF7F9FD)
    val Muted = Color(0xFFA8B5C7)
    val Blue = Color(0xFF4776FF)
    val Green = Color(0xFF78DBAF)
    val Amber = Color(0xFFF4B66B)
}

@Composable
fun AutomotiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = AutoColors.Blue, background = AutoColors.Ink,
        surface = AutoColors.Panel, surfaceVariant = AutoColors.Raised, onSurface = AutoColors.Text,
        onSurfaceVariant = AutoColors.Muted, onPrimary = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = AutoColors.Ink, contentColor = AutoColors.Text, content = content)
    }
}

/** Original Figma SVG, retained locally; preserve root aspect ratio and contain in slot. */
@Composable
fun FigmaAsset(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val svg = remember(name) { context.assets.open("automotive/$name.svg").use(SVG::getFromInputStream) }
    val picture = remember(svg) { svg.renderToPicture() }
    Canvas(modifier) {
        val scale = minOf(size.width / picture.width, size.height / picture.height)
        drawIntoCanvas { c ->
            val native = c.nativeCanvas
            native.save()
            native.translate((size.width - picture.width * scale) / 2, (size.height - picture.height * scale) / 2)
            native.scale(scale, scale)
            native.drawPicture(picture)
            native.restore()
        }
    }
}

@Composable
fun AutoButton(label: String, primary: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) AutoColors.Blue else AutoColors.Raised,
            contentColor = AutoColors.Text), modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AutoLabel(text: String, color: Color = AutoColors.Muted) {
    Text(text, color = color, fontSize = 17.sp, lineHeight = 26.sp)
}
