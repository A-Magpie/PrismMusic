package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.data.model.Song
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun QueueSheet(
    viewModel: MusicViewModel,
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentQueueIndex.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    // Drag-and-drop state with real-time incremental swapping
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeight = 60.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val listState = rememberLazyListState()

    // Stable tracking tokens per slot that swap with real-time reordering
    val itemTokens = remember { mutableStateListOf<Long>() }
    LaunchedEffect(queue.size) {
        if (draggedIndex == null || itemTokens.size != queue.size) {
            itemTokens.clear()
            itemTokens.addAll(queue.mapIndexed { idx, s -> s.id * 31L + idx })
        }
    }

    // Edge auto-scroll tracking that keeps the dragged item perfectly locked to the finger
    val currentDragOffsetY by androidx.compose.runtime.rememberUpdatedState(dragOffsetY)
    val currentDraggedIdx by androidx.compose.runtime.rememberUpdatedState(draggedIndex)
    LaunchedEffect(draggedIndex != null) {
        if (draggedIndex != null) {
            while (isActive && currentDraggedIdx != null) {
                val from = currentDraggedIdx ?: break
                val offset = currentDragOffsetY
                val threshold = itemHeightPx * 0.5f

                if (offset < -30f && from > 0) {
                    val scrollDelta = -14f
                    listState.scrollBy(scrollDelta)
                    dragOffsetY -= scrollDelta
                    if (dragOffsetY < -threshold && from > 0) {
                        val to = from - 1
                        if (from in itemTokens.indices && to in itemTokens.indices) {
                            val temp = itemTokens[from]
                            itemTokens[from] = itemTokens[to]
                            itemTokens[to] = temp
                        }
                        viewModel.reorderQueue(from, to)
                        draggedIndex = to
                        dragOffsetY += itemHeightPx
                    }
                } else if (offset > 30f && from < queue.size - 1) {
                    val scrollDelta = 14f
                    listState.scrollBy(scrollDelta)
                    dragOffsetY -= scrollDelta
                    if (dragOffsetY > threshold && from < queue.size - 1) {
                        val to = from + 1
                        if (from in itemTokens.indices && to in itemTokens.indices) {
                            val temp = itemTokens[from]
                            itemTokens[from] = itemTokens[to]
                            itemTokens[to] = temp
                        }
                        viewModel.reorderQueue(from, to)
                        draggedIndex = to
                        dragOffsetY -= itemHeightPx
                    }
                }
                delay(20)
            }
        }
    }

    // Dynamic finger-following pull-down to dismiss
    val sheetDragY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isOpen) {
        if (isOpen) {
            sheetDragY.snapTo(0f)
            if (currentIndex in queue.indices) {
                listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, sheetDragY.value.coerceAtLeast(0f).roundToInt()) }
                .graphicsLayer {
                    alpha = (1f - (sheetDragY.value / 1500f)).coerceIn(0.3f, 1f)
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            if (draggedIndex == null && (dragAmount.y > 0 || sheetDragY.value > 0)) {
                                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                    change.consume()
                                    coroutineScope.launch {
                                        sheetDragY.snapTo((sheetDragY.value + dragAmount.y).coerceAtLeast(0f))
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (sheetDragY.value > 80f) {
                                    sheetDragY.animateTo(2500f, androidx.compose.animation.core.tween(220))
                                    onClose()
                                } else {
                                    sheetDragY.animateTo(0f, androidx.compose.animation.core.tween(200))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                sheetDragY.animateTo(0f, androidx.compose.animation.core.tween(200))
                            }
                        }
                    )
                }
                .background(AmoledBlack)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("queue_sheet")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Swiping down on header dismisses the sheet easily)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    if (dragAmount.y > 0 || sheetDragY.value > 0) {
                                        change.consume()
                                        coroutineScope.launch {
                                            sheetDragY.snapTo((sheetDragY.value + dragAmount.y).coerceAtLeast(0f))
                                        }
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (sheetDragY.value > 80f) {
                                            sheetDragY.animateTo(2500f, androidx.compose.animation.core.tween(220))
                                            onClose()
                                        } else {
                                            sheetDragY.animateTo(0f, androidx.compose.animation.core.tween(200))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        sheetDragY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                }
                            )
                        }
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Play Queue",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${queue.size} tracks • Swipe down to return",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clear non-playing queue
                        if (queue.size > 1) {
                            IconButton(
                                onClick = {
                                    // Remove all except currently playing track
                                    val currentPlaying = currentSong
                                    if (currentPlaying != null) {
                                        viewModel.playSong(currentPlaying, listOf(currentPlaying))
                                    }
                                },
                                modifier = Modifier.testTag("btn_clear_queue")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Queue",
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSavePlaylistDialog = true },
                            modifier = Modifier.testTag("btn_save_queue_as_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Save Queue as Playlist",
                                tint = dynamicAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetDragY.animateTo(2500f, androidx.compose.animation.core.tween(220))
                                    onClose()
                                }
                            },
                            modifier = Modifier.testTag("btn_close_queue")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Queue",
                                tint = TextWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                if (queue.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Queue is empty", color = TextMuted, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = queue,
                            key = { index, song ->
                                if (index in itemTokens.indices) itemTokens[index] else (song.id * 31L + index)
                            }
                        ) { index, song ->
                            val isBeingDragged = (draggedIndex == index)

                            QueueItemRow(
                                song = song,
                                index = index,
                                currentIndex = currentIndex,
                                queueSize = queue.size,
                                isPlaying = (index == currentIndex && currentSong?.id == song.id),
                                isDragged = isBeingDragged,
                                accentColor = dynamicAccent,
                                dragOffsetY = if (isBeingDragged) dragOffsetY else 0f,
                                onPlay = { viewModel.playQueueIndex(index) },
                                onRemove = { viewModel.removeFromQueue(index) },
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { deltaY ->
                                    dragOffsetY += deltaY
                                    val from = draggedIndex ?: return@QueueItemRow
                                    val threshold = itemHeightPx * 0.5f
                                    if (dragOffsetY > threshold && from < queue.size - 1) {
                                        val to = from + 1
                                        if (from in itemTokens.indices && to in itemTokens.indices) {
                                            val temp = itemTokens[from]
                                            itemTokens[from] = itemTokens[to]
                                            itemTokens[to] = temp
                                        }
                                        viewModel.reorderQueue(from, to)
                                        draggedIndex = to
                                        dragOffsetY -= itemHeightPx
                                    } else if (dragOffsetY < -threshold && from > 0) {
                                        val to = from - 1
                                        if (from in itemTokens.indices && to in itemTokens.indices) {
                                            val temp = itemTokens[from]
                                            itemTokens[from] = itemTokens[to]
                                            itemTokens[to] = temp
                                        }
                                        viewModel.reorderQueue(from, to)
                                        draggedIndex = to
                                        dragOffsetY += itemHeightPx
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                // Mini Player embedded at bottom of QueueSheet
                MiniPlayer(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    onTap = {
                        onClose()
                        viewModel.isNowPlayingExpanded.value = true
                    }
                )
            }

            // Create Playlist from Queue Dialog
            if (showSavePlaylistDialog) {
                var playlistName by remember { mutableStateOf("Queue Playlist") }
                AlertDialog(
                    onDismissRequest = { showSavePlaylistDialog = false },
                    containerColor = DarkCardGlass,
                    title = {
                        Text(
                            text = "Save Queue as Playlist",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = dynamicAccent,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (playlistName.isNotBlank()) {
                                    viewModel.createPlaylistFromQueue(playlistName.trim())
                                    showSavePlaylistDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSavePlaylistDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    song: Song,
    index: Int,
    currentIndex: Int,
    queueSize: Int,
    isPlaying: Boolean,
    isDragged: Boolean,
    accentColor: Color,
    dragOffsetY: Float,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val isPlayed = index < currentIndex
    val isCurrent = (index == currentIndex)
    val itemAlpha = when {
        isDragged -> 1f
        isCurrent -> 1f
        isPlayed -> 0.40f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 10f else 1f)
            .offset { IntOffset(swipeOffset.roundToInt(), dragOffsetY.roundToInt()) }
            .scale(if (isDragged) 1.03f else 1.0f)
            .graphicsLayer {
                alpha = itemAlpha
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffset = (swipeOffset + dragAmount).coerceAtMost(0f)
                    },
                    onDragEnd = {
                        if (swipeOffset < -120f) {
                            onRemove()
                        }
                        swipeOffset = 0f
                    }
                )
            }
            .glassmorphic(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = when {
                    isDragged -> DarkCardGlass.copy(alpha = 0.95f)
                    isCurrent -> accentColor.copy(alpha = 0.22f)
                    isPlayed -> DarkCardGlass.copy(alpha = 0.35f)
                    else -> DarkCardGlass
                },
                borderColor = when {
                    isDragged -> accentColor
                    isCurrent -> accentColor.copy(alpha = 0.75f)
                    isPlayed -> GlassBorder.copy(alpha = 0.10f)
                    else -> GlassBorder.copy(alpha = 0.20f)
                }
            )
            .clickable { onPlay() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle: Immediate touch and drag locked to finger without interruption
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .pointerInput(song.id) {
                        detectDragGestures(
                            onDragStart = { currentOnDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDrag(dragAmount.y)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Hold and drag to reorder",
                    tint = when {
                        isDragged || isCurrent -> accentColor
                        isPlayed -> TextMuted.copy(alpha = 0.4f)
                        else -> TextMuted
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Compact Square Cover Art
            SquareCoverArt(
                albumArtUri = song.albumArtUri,
                contentDescription = song.title,
                shape = RoundedCornerShape(8.dp),
                showBorder = false,
                modifier = Modifier
                    .size(42.dp)
                    .then(if (isPlayed) Modifier.graphicsLayer { alpha = 0.50f } else Modifier)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Song Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = when {
                        isCurrent -> accentColor
                        isPlayed -> TextMuted
                        else -> TextWhite
                    },
                    fontSize = 14.sp,
                    fontWeight = when {
                        isCurrent -> FontWeight.Bold
                        isPlayed -> FontWeight.Normal
                        else -> FontWeight.Medium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = when {
                        isCurrent -> TextWhite
                        isPlayed -> TextMuted.copy(alpha = 0.60f)
                        else -> TextSecondary
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Active track badge
            if (isCurrent) {
                Text(
                    text = if (isPlaying) "PLAYING" else "PAUSED",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Swipe to remove hint indicator
            if (swipeOffset < -20f) {
                Text(
                    text = "Remove",
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
