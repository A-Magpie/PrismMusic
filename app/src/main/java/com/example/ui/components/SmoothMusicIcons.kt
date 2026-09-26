package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SmoothMusicIcons {

    val SmoothRepeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "SmoothRepeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Upper track looping clockwise from bottom-left to top-right
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Left vertical curve going up into top horizontal
                moveTo(6.5f, 14.5f)
                curveTo(6.5f, 9.5f, 7.5f, 6.5f, 12.5f, 6.5f)
                lineTo(18.0f, 6.5f)
                // Arrowhead pointing right-down at (18, 6.5)
                moveTo(15.5f, 4.0f)
                lineTo(18.5f, 6.5f)
                lineTo(15.5f, 9.0f)

                // Right vertical curve going down into bottom horizontal
                moveTo(17.5f, 9.5f)
                curveTo(17.5f, 14.5f, 16.5f, 17.5f, 11.5f, 17.5f)
                lineTo(6.0f, 17.5f)
                // Arrowhead pointing left-up at (6, 17.5)
                moveTo(8.5f, 20.0f)
                lineTo(5.5f, 17.5f)
                lineTo(8.5f, 15.0f)
            }
        }.build()
    }

    val SmoothRepeatOne: ImageVector by lazy {
        ImageVector.Builder(
            name = "SmoothRepeatOne",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Racetrack loop
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 14.5f)
                curveTo(6.5f, 9.5f, 7.5f, 6.5f, 12.5f, 6.5f)
                lineTo(18.0f, 6.5f)
                moveTo(15.5f, 4.0f)
                lineTo(18.5f, 6.5f)
                lineTo(15.5f, 9.0f)

                moveTo(17.5f, 9.5f)
                curveTo(17.5f, 14.5f, 16.5f, 17.5f, 11.5f, 17.5f)
                lineTo(6.0f, 17.5f)
                moveTo(8.5f, 20.0f)
                lineTo(5.5f, 17.5f)
                lineTo(8.5f, 15.0f)

                // Smooth centered "1" digit
                moveTo(11.0f, 10.5f)
                lineTo(12.2f, 9.5f)
                lineTo(12.2f, 14.5f)
            }
        }.build()
    }

    val SmoothShuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "SmoothShuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // First smooth S-curve from top-left to bottom-right
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 7.0f)
                lineTo(7.0f, 7.0f)
                curveTo(10.5f, 7.0f, 13.5f, 17.0f, 17.0f, 17.0f)
                lineTo(19.0f, 17.0f)

                // Arrow head at bottom-right
                moveTo(16.5f, 14.5f)
                lineTo(19.5f, 17.0f)
                lineTo(16.5f, 19.5f)
            }

            // Second smooth S-curve from bottom-left to top-right (crossing seamlessly)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 17.0f)
                lineTo(7.0f, 17.0f)
                curveTo(9.5f, 17.0f, 11.0f, 13.5f, 12.0f, 12.0f)

                moveTo(14.0f, 9.5f)
                curveTo(14.8f, 8.2f, 15.8f, 7.0f, 17.0f, 7.0f)
                lineTo(19.0f, 7.0f)

                // Arrow head at top-right
                moveTo(16.5f, 4.5f)
                lineTo(19.5f, 7.0f)
                lineTo(16.5f, 9.5f)
            }
        }.build()
    }
}
