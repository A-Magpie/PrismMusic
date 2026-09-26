package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.FloatingScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SortBy
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode

@Composable
fun DustyTracksTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val dustyTracks by viewModel.dustyTracks.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.dustySortBy.collectAsState()
    val sortDirection by viewModel.dustySortDirection.collectAsState()
    val layoutMode by viewModel.dustyLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val sortedDustyTracks = remember(dustyTracks, sortBy, sortDirection) {
        val sorted = when (sortBy) {
            SortBy.PLAY_COUNT -> dustyTracks.sortedBy { it.playCount }
            SortBy.TITLE -> dustyTracks.sortedBy { it.title.lowercase() }
            SortBy.DATE_MODIFIED -> dustyTracks.sortedBy { it.dateModified }
            SortBy.DATE_ADDED -> dustyTracks.sortedBy { it.dateAdded }
            SortBy.DURATION -> dustyTracks.sortedBy { it.duration }
        }
        if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 14.dp)
            .testTag("dusty_tracks_tab")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = AccentAmber)
                Text(
                    text = "Dusty Tracks",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Informational Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .glassmorphic(shape = RoundedCornerShape(12.dp), backgroundColor = DarkCardGlass)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Dusty Tracks Archive",
                        color = AccentAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tracks with low or zero plays (under 50% completed). Rediscover forgotten gems or free up storage space.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (sortedDustyTracks.isNotEmpty()) {
                Button(
                    onClick = { viewModel.playSong(sortedDustyTracks.first(), sortedDustyTracks) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Play Dusty Tracks", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            SortHeader(
                itemCount = sortedDustyTracks.size,
                sortBy = sortBy,
                sortDirection = sortDirection,
                onSortByChanged = { viewModel.setTabSort("dusty", newSortBy = it) },
                onToggleDirection = {
                    val newDir = if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                    viewModel.setTabSort("dusty", newSortDir = newDir)
                },
                layoutMode = layoutMode,
                onLayoutModeChanged = { viewModel.setLayoutMode("dusty", it) },
                accentColor = dynamicAccent
            )

            if (sortedDustyTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No dusty tracks found.\nAll your songs are played frequently!",
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
                                items(sortedDustyTracks, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedDustyTracks) },
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
                                items(sortedDustyTracks, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedDustyTracks) },
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
                                items(sortedDustyTracks, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedDustyTracks) },
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
                                items(sortedDustyTracks, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedDustyTracks) },
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

        songToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { songToAddToPlaylist = null }
            )
        }
    }
}
