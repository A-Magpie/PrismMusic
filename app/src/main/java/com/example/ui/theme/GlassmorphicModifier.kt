package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = DarkCardGlass,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
): Modifier = this
    .clip(shape)
    .background(
        color = backgroundColor,
        shape = shape
    )
