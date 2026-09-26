package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkSurfaceElevated

/**
 * A dedicated vertical equalizer fader/bar designed specifically for multi-band audio equalizers.
 * Provides touch-and-drag control, center 0dB indicator, smooth track, and pristine solid circle thumb.
 */
@Composable
fun VerticalEqualizerBar(
    value: Int, // -100 to 100
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
    activeColor: Color = AccentCyan,
    inactiveColor: Color = DarkSurfaceElevated,
    thumbColor: Color = AccentCyan,
    width: Dp = 36.dp,
    height: Dp = 140.dp,
    thumbRadius: Dp = 7.dp,
    trackWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val currentOnValueChange = rememberUpdatedState(onValueChange)

    fun calculateValueFromY(y: Float, canvasHeight: Float, thumbPadding: Float): Int {
        val usableHeight = canvasHeight - 2 * thumbPadding
        if (usableHeight <= 0f) return 0
        val clampedY = (y - thumbPadding).coerceIn(0f, usableHeight)
        // y = 0 is top (+100), y = usableHeight is bottom (-100)
        val fraction = 1f - (clampedY / usableHeight)
        val calculated = (fraction * 200f - 100f).toInt()
        return calculated.coerceIn(-100, 100)
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val newVal = calculateValueFromY(offset.y, size.height.toFloat(), thumbRadius.toPx())
                    currentOnValueChange.value(newVal)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val newVal = calculateValueFromY(change.position.y, size.height.toFloat(), thumbRadius.toPx())
                    currentOnValueChange.value(newVal)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val thumbR = thumbRadius.toPx()
            val trackW = trackWidth.toPx()
            val topY = thumbR
            val bottomY = size.height - thumbR
            val usableH = bottomY - topY
            val centerY = topY + usableH / 2f

            // 1. Inactive full vertical track line
            drawLine(
                color = if (enabled) inactiveColor else Color(0x22FFFFFF),
                start = Offset(cx, topY),
                end = Offset(cx, bottomY),
                strokeWidth = trackW,
                cap = StrokeCap.Round
            )

            // 2. Center 0dB tick mark
            drawLine(
                color = if (enabled) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                start = Offset(cx - 8f, centerY),
                end = Offset(cx + 8f, centerY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // 3. Thumb position
            // fraction: -100 -> 0f, 0 -> 0.5f, 100 -> 1f
            val fraction = ((value.coerceIn(-100, 100) + 100) / 200f).coerceIn(0f, 1f)
            // topY is +100 (fraction 1), bottomY is -100 (fraction 0)
            val thumbY = bottomY - (fraction * usableH)

            // 4. Active track from center 0dB to thumb
            if (enabled) {
                drawLine(
                    color = activeColor,
                    start = Offset(cx, centerY),
                    end = Offset(cx, thumbY),
                    strokeWidth = trackW,
                    cap = StrokeCap.Round
                )
            }

            // 5. Solid Circle Thumb (no background, pristine circle)
            drawCircle(
                color = if (enabled) thumbColor else Color.Gray,
                radius = thumbR,
                center = Offset(cx, thumbY)
            )
        }
    }
}
