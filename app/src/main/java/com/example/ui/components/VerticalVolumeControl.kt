package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VerticalVolumeControl(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var previousNonZeroVolume by remember {
        mutableIntStateOf(if (currentVolume > 0) currentVolume else (maxVolume / 2).coerceAtLeast(1))
    }

    // Periodically poll hardware volume in case user pressed hardware rocker keys
    LaunchedEffect(Unit) {
        while (isActive) {
            val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (vol != currentVolume) {
                currentVolume = vol
                if (vol > 0) previousNonZeroVolume = vol
            }
            delay(400)
        }
    }

    val isMuted = currentVolume == 0
    val volumeFraction = (currentVolume.toFloat() / maxVolume).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .width(42.dp)
            .glassmorphic(shape = RoundedCornerShape(22.dp), backgroundColor = DarkCardGlass)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Vertical Slider Track
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .width(28.dp)
                .pointerInput(maxVolume) {
                    detectTapGestures { offset ->
                        val trackH = size.height.toFloat()
                        if (trackH > 0) {
                            val rawFraction = 1f - (offset.y / trackH)
                            val targetVol = (rawFraction.coerceIn(0f, 1f) * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            currentVolume = targetVol
                            if (targetVol > 0) previousNonZeroVolume = targetVol
                        }
                    }
                }
                .pointerInput(maxVolume) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val trackH = size.height.toFloat()
                        if (trackH > 0) {
                            val rawFraction = 1f - (change.position.y / trackH)
                            val targetVol = (rawFraction.coerceIn(0f, 1f) * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            currentVolume = targetVol
                            if (targetVol > 0) previousNonZeroVolume = targetVol
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val trackHeight = maxHeight

            // Inactive Track background
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33FFFFFF))
            )

            // Active fill track from bottom
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(trackHeight * volumeFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Sleek Round Thumb
            Box(
                modifier = Modifier
                    .padding(bottom = (trackHeight * volumeFraction - 8.dp).coerceAtLeast(0.dp))
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TextWhite)
            )
        }

        // Mute / Unmute Button at bottom
        IconButton(
            onClick = {
                if (currentVolume > 0) {
                    previousNonZeroVolume = currentVolume
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    currentVolume = 0
                } else {
                    val restoreVol = previousNonZeroVolume.coerceIn(1, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVol, 0)
                    currentVolume = restoreVol
                }
            },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = when {
                    isMuted -> Icons.Default.VolumeOff
                    volumeFraction < 0.4f -> Icons.AutoMirrored.Filled.VolumeMute
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) TextMuted else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
