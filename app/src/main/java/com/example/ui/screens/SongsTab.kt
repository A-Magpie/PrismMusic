package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.FloatingScrollbar
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode
import kotlinx.coroutines.launch

@Composable
fun SongsTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.sortedSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()
    val layoutMode by viewModel.songsLayoutMode.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Pull-to-refresh implementation with fling protection and ceiling requirement
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val pullOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val pullThreshold = with(LocalDensity.current) { 160.dp.toPx() }

    val nestedScrollConnection = remember(layoutMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Fling motions should NEVER accumulate pull-to-refresh offset
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                if (available.y > 0 && pullOffset.value > 0) {
                    coroutineScope.launch {
                        pullOffset.snapTo((pullOffset.value + available.y * 0.30f).coerceAtMost(pullThreshold * 1.5f))
                    }
                    return Offset(0f, available.y)
                }
                if (available.y < 0 && pullOffset.value > 0) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Strictly UserInput: flinging to top will NEVER trigger refresh
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val isAtCeiling = when (layoutMode) {
                    ViewLayoutMode.LIST_NORMAL, ViewLayoutMode.LIST_COMPACT ->
                        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    ViewLayoutMode.GRID_3, ViewLayoutMode.GRID_4 ->
                        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                }
                if (available.y > 0 && isAtCeiling) {
                    coroutineScope.launch {
                        pullOffset.snapTo((pullOffset.value + available.y * 0.30f).coerceAtMost(pullThreshold * 1.5f))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value >= pullThreshold && !isScanning) {
                    viewModel.refreshMedia()
                }
                coroutineScope.launch {
                    pullOffset.animateTo(0f, tween(250))
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 14.dp)
            .nestedScroll(nestedScrollConnection)
            .testTag("songs_tab")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Tracks",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pull-down refresh spinner indicator
            AnimatedVisibility(
                visible = pullOffset.value > 8f || isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { if (isScanning) 1f else (pullOffset.value / pullThreshold).coerceIn(0.1f, 1f) },
                        color = dynamicAccent,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }

            // Quick Play All / Shuffle All bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            viewModel.playSong(songs.first(), songs)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            viewModel.playSong(shuffled.first(), shuffled)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = dynamicAccent)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Shuffle", color = dynamicAccent, fontWeight = FontWeight.Bold)
                }
            }

            // Sort & View Mode Header
            SortHeader(
                itemCount = songs.size,
                sortBy = sortBy,
                sortDirection = sortDirection,
                onSortByChanged = { viewModel.sortBy.value = it },
                onToggleDirection = {
                    viewModel.sortDirection.value =
                        if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                        else SortDirection.ASCENDING
                },
                layoutMode = layoutMode,
                onLayoutModeChanged = { viewModel.songsLayoutMode.value = it },
                accentColor = dynamicAccent
            )

            // Songs List or Grid
            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs found.\nPull down to scan device files.",
                        color = TextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (layoutMode) {
                    ViewLayoutMode.LIST_NORMAL -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                SongItemCard(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = (currentSong?.id == song.id),
                                    onClick = { viewModel.playSong(song, songs) },
                                    onPlayNext = { viewModel.playNextInQueue(song) },
                                    onAddToQueue = { viewModel.addToQueueEnd(song) },
                                    onAddToPlaylist = { songToAddToPlaylist = song },
                                    onEditTags = { viewModel.songForTagEditor.value = song },
                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                    onDelete = { viewModel.deleteSong(song) },
                                    accentColor = dynamicAccent,
                                    isCompact = false
                                )
                            }
                        }
                    }
                    ViewLayoutMode.LIST_COMPACT -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                SongItemCard(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = (currentSong?.id == song.id),
                                    onClick = { viewModel.playSong(song, songs) },
                                    onPlayNext = { viewModel.playNextInQueue(song) },
                                    onAddToQueue = { viewModel.addToQueueEnd(song) },
                                    onAddToPlaylist = { songToAddToPlaylist = song },
                                    onEditTags = { viewModel.songForTagEditor.value = song },
                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                    onDelete = { viewModel.deleteSong(song) },
                                    accentColor = dynamicAccent,
                                    isCompact = true
                                )
                            }
                        }
                    }
                    ViewLayoutMode.GRID_3 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                SongGridItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = (currentSong?.id == song.id),
                                    onClick = { viewModel.playSong(song, songs) },
                                    accentColor = dynamicAccent,
                                    isCompactGrid = false
                                )
                            }
                        }
                    }
                    ViewLayoutMode.GRID_4 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(songs, key = { it.id }) { song ->
                                SongGridItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = (currentSong?.id == song.id),
                                    onClick = { viewModel.playSong(song, songs) },
                                    accentColor = dynamicAccent,
                                    isCompactGrid = true
                                )
                            }
                        }
                    }
                }
                if (layoutMode == ViewLayoutMode.LIST_NORMAL || layoutMode == ViewLayoutMode.LIST_COMPACT) {
                        FloatingScrollbar(
                            listState = listState,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            accentColor = dynamicAccent
                        )
                    } else {
                        FloatingScrollbar(
                            gridState = gridState,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            accentColor = dynamicAccent
                        )
                    }
                }
            }
        }

        // Add to Playlist Dialog
        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { songToAddToPlaylist = null }
            )
        }
    }
}
