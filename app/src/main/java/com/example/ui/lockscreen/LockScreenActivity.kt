package com.example.ui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.playback.AudioPlaybackService
import com.example.ui.components.SleekSliderTrack
import com.example.ui.components.SolidCircleThumb
import com.example.ui.components.SquareCoverArt
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class LockScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make truly full-screen: hide system bars, layout behind cutouts
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Show when locked & turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            MyApplicationTheme {
                LockScreenContent(
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LockScreenContent(
    onDismiss: () -> Unit
) {
    val service = AudioPlaybackService.instance
    val currentSong = service?.currentSong
    val isPlaying = service?.playerEngine?.isPlaying?.collectAsState()?.value ?: false
    val position = service?.playerEngine?.currentPosition?.collectAsState()?.value ?: 0L
    val duration = service?.playerEngine?.duration?.collectAsState()?.value ?: (currentSong?.duration ?: 0L)

    // Clock state
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (isActive) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000)
        }
    }

    // Settings for touch lock
    var appSettings by remember { mutableStateOf(AppSettingsEntity()) }
    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(service?.applicationContext ?: return@LaunchedEffect)
        db.appSettingsDao().getSettings().collect {
            if (it != null) appSettings = it
        }
    }

    // Touch Lock Feature
    var isTouchLocked by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    val swipeOffsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newX = (swipeOffsetX.value + dragAmount).coerceAtLeast(0f)
                            swipeOffsetX.snapTo(newX)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (swipeOffsetX.value > 160f) {
                                swipeOffsetX.animateTo(2500f, spring(stiffness = Spring.StiffnessMediumLow))
                                onDismiss()
                            } else {
                                swipeOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            swipeOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                )
            }
            .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                alpha = (1f - (swipeOffsetX.value / 1200f)).coerceIn(0.1f, 1f)
            }
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag("lock_screen_content")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clock (Placed high at top, larger font: 62sp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = currentTimeString,
                    color = TextWhite,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Text(
                    text = currentDateString,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Square Cover Art
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SquareCoverArt(
                    albumArtUri = currentSong?.albumArtUri,
                    contentDescription = currentSong?.title,
                    shape = RoundedCornerShape(20.dp),
                    showBorder = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Song Info (Title & Artist)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentSong?.title ?: "No Track Playing",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentSong?.artist ?: "Local Audio Player",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seekbar
            Column(modifier = Modifier.fillMaxWidth()) {
                val totalDur = if (duration > 0) duration else (currentSong?.duration ?: 1L)
                val currentPos = if (isSeeking) (seekProgress * totalDur).toLong() else position
                val progressFloat = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

                val seekbarEnabled = !(isTouchLocked && appSettings.lockDisableSeekbar)

                Slider(
                    value = progressFloat,
                    onValueChange = {
                        if (seekbarEnabled) {
                            isSeeking = true
                            seekProgress = it
                        }
                    },
                    onValueChangeFinished = {
                        if (seekbarEnabled) {
                            val seekTarget = (seekProgress * totalDur).toLong()
                            service?.playerEngine?.seekTo(seekTarget)
                            isSeeking = false
                        }
                    },
                    enabled = seekbarEnabled,
                    track = { sliderState ->
                        SleekSliderTrack(
                            sliderState = sliderState,
                            activeTrackColor = if (seekbarEnabled) AccentCyan else TextMuted,
                            inactiveTrackColor = Color(0x33FFFFFF),
                            trackHeight = 4.dp
                        )
                    },
                    thumb = {
                        SolidCircleThumb(
                            color = if (seekbarEnabled) AccentCyan else TextMuted,
                            size = 16.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(currentPos), color = TextMuted, fontSize = 11.sp)
                    Text(text = formatDuration(totalDur), color = TextMuted, fontSize = 11.sp)
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prevNextEnabled = !(isTouchLocked && appSettings.lockDisablePrevNext)
                val playPauseEnabled = !(isTouchLocked && appSettings.lockDisablePlayPause)

                // Prev
                IconButton(
                    onClick = { if (prevNextEnabled) service?.playPrevious() },
                    enabled = prevNextEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (prevNextEnabled) TextWhite else TextMuted,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (playPauseEnabled) {
                                Brush.linearGradient(listOf(AccentCyan, AccentPink))
                            } else {
                                Brush.linearGradient(listOf(DarkSurfaceElevated, DarkSurfaceElevated))
                            }
                        )
                        .clickable(enabled = playPauseEnabled) {
                            service?.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (playPauseEnabled) Color.Black else TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { if (prevNextEnabled) service?.playNext() },
                    enabled = prevNextEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (prevNextEnabled) TextWhite else TextMuted,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Center-Bottom: Touch Lock toggle button (long-press 1s) and swipe right hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isTouchLocked) AccentRed.copy(alpha = 0.30f) else Color(0x22FFFFFF))
                        .pointerInput(isTouchLocked) {
                            detectTapGestures(
                                onLongPress = {
                                    isTouchLocked = !isTouchLocked
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTouchLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isTouchLocked) "Touch Locked" else "Touch Unlocked",
                        tint = if (isTouchLocked) AccentRed else TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isTouchLocked) "Touch Locked • Hold padlock to unlock" else "Hold padlock to lock • Swipe right to unlock",
                    color = TextMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(mins, secs)
}
