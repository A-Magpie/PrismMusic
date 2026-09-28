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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SortBy
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode

@Composable
fun FavoritesTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favoriteSongs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.favoritesSortBy.collectAsState()
    val sortDirection by viewModel.favoritesSortDirection.collectAsState()
    val layoutMode by viewModel.favoritesLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val sortedFavorites = remember(favorites, sortBy, sortDirection) {
        val sorted = when (sortBy) {
            SortBy.TITLE -> favorites.sortedBy { it.title.lowercase() }
            SortBy.DATE_MODIFIED -> favorites.sortedBy { it.dateModified }
            SortBy.DATE_ADDED -> favorites.sortedBy { it.dateAdded }
            SortBy.DURATION -> favorites.sortedBy { it.duration }
            SortBy.PLAY_COUNT -> favorites.sortedBy { it.playCount }
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
            .testTag("favorites_tab")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentRed)
                Text(
                    text = "Favorites",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (sortedFavorites.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.playSong(sortedFavorites.first(), sortedFavorites) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Play All", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (sortedFavorites.isNotEmpty()) {
                                viewModel.shufflePlaySongs(sortedFavorites)
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
            }

            SortHeader(
                itemCount = sortedFavorites.size,
                sortBy = sortBy,
                sortDirection = sortDirection,
                onSortByChanged = { viewModel.setTabSort("favorites", newSortBy = it) },
                onToggleDirection = {
                    val newDir = if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                    viewModel.setTabSort("favorites", newSortDir = newDir)
                },
                layoutMode = layoutMode,
                onLayoutModeChanged = { viewModel.setLayoutMode("favorites", it) },
                accentColor = dynamicAccent
            )

            if (sortedFavorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No favorite tracks yet.\nTap the heart icon on any song to add it here.",
                        color = TextMuted,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                items(sortedFavorites, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedFavorites) },
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
                                items(sortedFavorites, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedFavorites) },
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
                                items(sortedFavorites, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedFavorites) },
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
                                items(sortedFavorites, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, sortedFavorites) },
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
