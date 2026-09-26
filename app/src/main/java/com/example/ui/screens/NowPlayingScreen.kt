package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import com.example.ui.components.SmoothMusicIcons
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.example.ui.components.SquareCoverArt
import com.example.ui.components.SolidCircleThumb
import com.example.ui.components.SleekSliderTrack
import com.example.ui.components.VerticalVolumeControl
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val showLyrics by viewModel.showLyrics.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()

    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    // Dynamic finger-following interactive gesture (both vertical and horizontal)
    val dragOffsetY = remember { Animatable(0f) }
    val dragOffsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isNowPlayingExpanded) {
        if (isNowPlayingExpanded) {
            dragOffsetY.snapTo(0f)
            dragOffsetX.snapTo(0f)
        }
    }

    var dragDirection by remember { mutableStateOf<String?>(null) }
    var totalDx by remember { mutableFloatStateOf(0f) }
    var totalDy by remember { mutableFloatStateOf(0f) }

    val currentDragY = dragOffsetY.value.coerceAtLeast(0f)
    val dragProgress = (currentDragY / 800f).coerceIn(0f, 1f)
    val dynamicScale = 1f - (dragProgress * 0.12f)
    val dynamicAlpha = 1f - (dragProgress * 0.40f)
    val dynamicCornerRadius = (dragProgress * 28f).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .testTag("now_playing_screen")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                        dragDirection = null
                    },
                    onDrag = { change, dragAmount ->
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y

                        if (dragDirection == null) {
                            val absX = kotlin.math.abs(totalDx)
                            val absY = kotlin.math.abs(totalDy)
                            // Human thumb arc: prioritize vertical if vertical displacement is substantial
                            if (absY > 14f && absY > absX * 0.70f) {
                                dragDirection = "V"
                            } else if (absX > 14f && absX > absY * 0.70f) {
                                dragDirection = "H"
                            }
                        }

                        if (dragDirection != null) {
                            change.consume()
                            coroutineScope.launch {
                                when (dragDirection) {
                                    "V" -> {
                                        val newY = dragOffsetY.value + dragAmount.y
                                        dragOffsetY.snapTo(newY.coerceAtLeast(-80f))
                                    }
                                    "H" -> {
                                        val newX = dragOffsetX.value + dragAmount.x
                                        dragOffsetX.snapTo(newX)
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            when (dragDirection) {
                                "V" -> {
                                    // Smooth lower threshold for comfortable swipe
                                    if (dragOffsetY.value > 65f) {
                                        dragOffsetY.animateTo(
                                            targetValue = 2500f,
                                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                                        )
                                        viewModel.isNowPlayingExpanded.value = false
                                    } else if (dragOffsetY.value < -50f) {
                                        viewModel.isQueueOpen.value = true
                                        dragOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                    } else {
                                        dragOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                    }
                                }
                                "H" -> {
                                    val threshold = 70f
                                    if (dragOffsetX.value < -threshold) {
                                        // Left swipe: next song
                                        dragOffsetX.animateTo(-1000f, tween(180, easing = FastOutSlowInEasing))
                                        viewModel.playNext()
                                        dragOffsetX.snapTo(0f)
                                    } else if (dragOffsetX.value > threshold) {
                                        // Right swipe: previous song
                                        dragOffsetX.animateTo(1000f, tween(180, easing = FastOutSlowInEasing))
                                        viewModel.playPrevious()
                                        dragOffsetX.snapTo(0f)
                                    } else {
                                        dragOffsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                            dragDirection = null
                            totalDx = 0f
                            totalDy = 0f
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                            dragOffsetX.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                            dragDirection = null
                            totalDx = 0f
                            totalDy = 0f
                        }
                    }
                )
            }
            .offset { IntOffset(0, currentDragY.roundToInt()) }
            .graphicsLayer {
                scaleX = dynamicScale
                scaleY = dynamicScale
                alpha = dynamicAlpha
                clip = true
                shape = RoundedCornerShape(dynamicCornerRadius)
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar (No handle bar per user mandate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(2500f, tween(220, easing = FastOutSlowInEasing))
                            viewModel.isNowPlayingExpanded.value = false
                        }
                    },
                    modifier = Modifier.testTag("btn_collapse_now_playing")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse to Mini Player",
                        tint = TextWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    color = dynamicAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                IconButton(
                    onClick = { viewModel.isQueueOpen.value = true },
                    modifier = Modifier.testTag("btn_open_queue")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Previous and Next song references for real-time dual-cover swipe
            val prevSong = remember(queue, currentQueueIndex) {
                if (currentQueueIndex > 0 && currentQueueIndex - 1 in queue.indices) {
                    queue[currentQueueIndex - 1]
                } else null
            }
            val nextSong = remember(queue, currentQueueIndex) {
                if (currentQueueIndex + 1 in queue.indices) {
                    queue[currentQueueIndex + 1]
                } else null
            }

            // Cover Art Container: Real-time dual-cover swipe, square, no borders, smooth track switch
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val coverWidth = maxWidth * 0.96f
                val spacingPx = with(androidx.compose.ui.platform.LocalDensity.current) { (maxWidth * 1.04f).toPx() }

                // Next cover entering from right when dragging left
                if (nextSong != null && dragOffsetX.value < -2f) {
                    Box(
                        modifier = Modifier
                            .width(coverWidth)
                            .aspectRatio(1f)
                            .offset { IntOffset((dragOffsetX.value + spacingPx).roundToInt(), 0) }
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        SquareCoverArt(
                            albumArtUri = nextSong.albumArtUri,
                            contentDescription = nextSong.title,
                            showBorder = false,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Previous cover entering from left when dragging right
                if (prevSong != null && dragOffsetX.value > 2f) {
                    Box(
                        modifier = Modifier
                            .width(coverWidth)
                            .aspectRatio(1f)
                            .offset { IntOffset((dragOffsetX.value - spacingPx).roundToInt(), 0) }
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        SquareCoverArt(
                            albumArtUri = prevSong.albumArtUri,
                            contentDescription = prevSong.title,
                            showBorder = false,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Current Song Cover (Center)
                Box(
                    modifier = Modifier
                        .width(coverWidth)
                        .aspectRatio(1f)
                        .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                        .clip(RoundedCornerShape(18.dp))
                        .pointerInput(currentSong?.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // Double-tap cover art to display embedded lyrics
                                    viewModel.showLyrics.value = !viewModel.showLyrics.value
                                }
                            )
                        }
                ) {
                    SquareCoverArt(
                        albumArtUri = currentSong?.albumArtUri,
                        contentDescription = currentSong?.title ?: "Cover Art",
                        showBorder = false,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Embedded Lyrics Overlay (Center-aligned over cover, double-tap toggles)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLyrics,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LyricsOverlay(
                            lyrics = currentSong?.lyrics ?: "",
                            currentPos = position,
                            fontSizeSetting = lyricsFontSize,
                            accentColor = dynamicAccent,
                            onFontSizeChange = { viewModel.lyricsFontSize.value = it },
                            onClose = { viewModel.showLyrics.value = false }
                        )
                    }
                }
            }

            // Song Info (Title, Artist, Album) + Vertical Volume Slider + 3 Vertical Buttons (Fav, Speed, EQ)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title, Artist, Album with smooth transition animation
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    AnimatedContent(
                        targetState = currentSong,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 2 })
                                .togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                        },
                        label = "track_title_anim"
                    ) { song ->
                        Column {
                            Text(
                                text = song?.title ?: "Select a Track",
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song?.artist ?: "Unknown Artist",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Vertical Volume Control beside the 4 vertical buttons, matching their 188dp height!
                VerticalVolumeControl(
                    accentColor = dynamicAccent,
                    modifier = Modifier.height(188.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Right: 4 buttons vertically aligned (Favorite, Playback Speed, EQ, Tag Editor) - height 188dp!
                Column(
                    modifier = Modifier
                        .height(188.dp)
                        .glassmorphic(shape = RoundedCornerShape(22.dp), backgroundColor = DarkCardGlass)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Favorite (Heart)
                    val isFav = currentSong?.isFavorite == true
                    IconButton(
                        onClick = { currentSong?.let { viewModel.toggleFavorite(it) } },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_favorite_side")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) AccentRed else TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 2. Playback Speed
                    IconButton(
                        onClick = {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            val currentIdx = speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.05f }
                            val nextSpeed = speeds[(currentIdx + 1) % speeds.size]
                            viewModel.setSpeed(nextSpeed)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_playback_speed")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Playback Speed",
                                tint = if (speed != 1.0f) dynamicAccent else TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "%.2fx".format(speed).replace(".00", ""),
                                color = if (speed != 1.0f) dynamicAccent else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3. 10-Band Equalizer
                    IconButton(
                        onClick = { viewModel.isEqualizerOpen.value = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_equalizer_side")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "10-Band Equalizer",
                            tint = dynamicAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 4. Tag Editor
                    IconButton(
                        onClick = { currentSong?.let { viewModel.songForTagEditor.value = it } },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_edit_tags_now_playing")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Tags",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Seekbar and Timestamp Indicators + Next Track Title
            Column(modifier = Modifier.fillMaxWidth()) {
                val totalDur = if (duration > 0) duration else (currentSong?.duration ?: 1L)
                val currentPos = if (isSeeking) (seekProgress * totalDur).toLong() else position
                val progressFloat = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

                Slider(
                    value = progressFloat,
                    onValueChange = {
                        isSeeking = true
                        seekProgress = it
                    },
                    onValueChangeFinished = {
                        val seekTarget = (seekProgress * totalDur).toLong()
                        viewModel.seekTo(seekTarget)
                        isSeeking = false
                    },
                    track = { sliderState ->
                        SleekSliderTrack(
                            sliderState = sliderState,
                            activeTrackColor = dynamicAccent,
                            inactiveTrackColor = Color(0x33FFFFFF),
                            trackHeight = 4.dp
                        )
                    },
                    thumb = {
                        SolidCircleThumb(color = dynamicAccent, size = 16.dp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seekbar")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPos),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(totalDur),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Next track title and artist
                val nextSong = remember(queue, currentQueueIndex) {
                    val nextIdx = currentQueueIndex + 1
                    if (nextIdx in queue.indices) queue[nextIdx] else null
                }
                Text(
                    text = if (nextSong != null) "Next: ${nextSong.title} • ${nextSong.artist}" else "Next: None (End of Queue)",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }

            // Bottom Controls: unified Play/Pause toggle in glassmorphic bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(28.dp), backgroundColor = DarkCardGlass)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat Button (Left)
                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.testTag("btn_repeat")
                ) {
                    Icon(
                        imageVector = if (repeatMode == 2) SmoothMusicIcons.SmoothRepeatOne else SmoothMusicIcons.SmoothRepeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) dynamicAccent else TextMuted
                    )
                }

                // Prev Button
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_prev")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Unified Play/Pause Toggle Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(dynamicAccent, dynamicAccent.copy(alpha = 0.85f))
                            )
                        )
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("btn_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Shuffle Button (Right)
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("btn_shuffle")
                ) {
                    Icon(
                        imageVector = SmoothMusicIcons.SmoothShuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) dynamicAccent else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsOverlay(
    lyrics: String,
    currentPos: Long,
    fontSizeSetting: String,
    accentColor: Color = AccentCyan,
    onFontSizeChange: (String) -> Unit,
    onClose: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val baseFontSize = when (fontSizeSetting) {
        "SMALL" -> 14.sp
        "LARGE" -> 22.sp
        else -> 17.sp // MEDIUM
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE080810))
            .padding(16.dp)
    ) {
        // 3-dot menu top-right for font size options
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Lyrics Options",
                    tint = TextWhite
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(DarkCardGlass)
            ) {
                DropdownMenuItem(
                    text = { Text("Font: Small", color = TextWhite) },
                    onClick = {
                        onFontSizeChange("SMALL")
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Font: Medium", color = TextWhite) },
                    onClick = {
                        onFontSizeChange("MEDIUM")
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Font: Large", color = TextWhite) },
                    onClick = {
                        onFontSizeChange("LARGE")
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Close Lyrics", color = accentColor) },
                    onClick = {
                        menuExpanded = false
                        onClose()
                    }
                )
            }
        }

        // Lyrics Text Content (Synced or Static, center-aligned over cover)
        val scrollState = rememberScrollState()

        if (lyrics.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No embedded lyrics available.\nDouble-tap to dismiss.",
                    color = TextMuted,
                    fontSize = baseFontSize,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Check for timestamps e.g. [00:12.34]
            val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
            val lines = lyrics.lines()
            val hasTimestamps = lines.any { timestampRegex.matches(it.trim()) }

            if (hasTimestamps) {
                val parsedLines = lines.mapNotNull { line ->
                    val match = timestampRegex.find(line.trim())
                    if (match != null) {
                        val mins = match.groupValues[1].toLongOrNull() ?: 0L
                        val secs = match.groupValues[2].toLongOrNull() ?: 0L
                        val ms = match.groupValues[3].toLongOrNull() ?: 0L
                        val timeMs = mins * 60000 + secs * 1000 + (if (ms < 100) ms * 10 else ms)
                        val text = match.groupValues[4].trim()
                        Pair(timeMs, text)
                    } else null
                }

                // Find active line index
                val activeIndex = parsedLines.indexOfLast { it.first <= currentPos }.coerceAtLeast(0)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 36.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    parsedLines.forEachIndexed { idx, item ->
                        val isActive = idx == activeIndex
                        Text(
                            text = item.second.ifBlank { "• • •" },
                            color = if (isActive) accentColor else TextMuted,
                            fontSize = if (isActive) (baseFontSize.value + 2).sp else baseFontSize,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            } else {
                // Static lyrics
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 36.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            color = TextWhite,
                            fontSize = baseFontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
