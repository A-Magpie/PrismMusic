package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan

/**
 * A pristine solid circle thumb for seekbars and sliders.
 * Drawn via Canvas to ensure pure transparency with zero bounding box or dark background artifacts.
 */
@Composable
fun SolidCircleThumb(
    color: Color = AccentCyan,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        drawCircle(
            color = color,
            radius = size.toPx() / 2f
        )
    }
}

/**
 * A continuous, sleek rounded track for Compose Slider.
 * Completely eliminates Material 3's default rectangular stop-indicator cutouts/gaps
 * which appear as black box gaps in dark theme layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleekSliderTrack(
    sliderState: SliderState,
    activeTrackColor: Color = AccentCyan,
    inactiveTrackColor: Color = Color(0x33FFFFFF),
    trackHeight: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        val y = size.height / 2f
        val strokeW = size.height
        val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        val fraction = if (range > 0f) {
            ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
        } else 0f
        val activeEnd = size.width * fraction

        // Inactive track (full width)
        drawLine(
            color = inactiveTrackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Active track (from 0 to thumb position)
        if (activeEnd > 0f) {
            drawLine(
                color = activeTrackColor,
                start = Offset(0f, y),
                end = Offset(activeEnd, y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
