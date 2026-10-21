package com.klvcorp.lumio.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HueWheel(argb: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argb, hsv)
    val currentColor = rememberUpdatedState(argb)
    val currentOnChange = rememberUpdatedState(onChange)
    val colors = (0..12).map { i -> Color(AndroidColor.HSVToColor(floatArrayOf((i * 30 % 360).toFloat(), 1f, 1f))) }
    fun pick(pos: Offset, size: androidx.compose.ui.unit.IntSize) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val angle = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble()))
        val hue = ((angle + 360) % 360).toFloat()
        val liveHsv = FloatArray(3)
        AndroidColor.colorToHSV(currentColor.value, liveHsv)
        currentOnChange.value(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    }
    Canvas(modifier
        .pointerInput(Unit) { detectTapGestures { pick(it, size) } }
        .pointerInput(Unit) { detectDragGestures(onDragStart = { pick(it, size) }, onDrag = { change, _ -> pick(change.position, size) }) }
    ) {
        val radius = min(size.width, size.height) / 2f - 24.dp.toPx()
        drawCircle(brush = Brush.sweepGradient(colors), radius = radius, style = Stroke(width = 35.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = Color(argb), radius = radius * 0.58f)
        val angle = Math.toRadians(hsv[0].toDouble())
        val marker = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
        drawCircle(Color.White, radius = 11.dp.toPx(), center = marker)
        drawCircle(Color(argb), radius = 7.dp.toPx(), center = marker)
    }
}

@Composable
fun PresetDot(color: Int, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
        else androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(color))
    ) { }
}
