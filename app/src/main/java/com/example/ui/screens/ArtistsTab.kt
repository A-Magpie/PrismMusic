package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.FloatingScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode

@Composable
fun ArtistsTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allSongs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val layoutMode by viewModel.artistsLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Hardware and Gesture Back Handler: Return to artist list instead of switching tabs or closing app
    BackHandler(enabled = selectedArtist != null) {
        viewModel.selectedArtist.value = null
    }

    // Group songs by artist
    val artists = remember(songs, sortBy, sortDirection) {
        val grouped = songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }
        val artistList = grouped.map { (artistName, trackList) ->
            ArtistItem(name = artistName, tracks = trackList)
        }
        val sorted = when (sortBy) {
            com.example.ui.viewmodel.SortBy.TITLE -> artistList.sortedBy { it.name.lowercase() }
            com.example.ui.viewmodel.SortBy.DATE_MODIFIED -> artistList.sortedBy { it.tracks.maxOfOrNull { s -> s.dateModified } ?: 0L }
            com.example.ui.viewmodel.SortBy.DATE_ADDED -> artistList.sortedBy { it.tracks.maxOfOrNull { s -> s.dateAdded } ?: 0L }
            com.example.ui.viewmodel.SortBy.DURATION -> artistList.sortedBy { it.tracks.sumOf { s -> s.duration } }
            com.example.ui.viewmodel.SortBy.PLAY_COUNT -> artistList.sortedBy { it.tracks.sumOf { s -> s.playCount } }
        }
        if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    val detailListState = rememberLazyListState()
    val detailGridState = rememberLazyGridState()
    val rootListState = rememberLazyListState()
    val rootGridState = rememberLazyGridState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 14.dp)
            .testTag("artists_tab")
    ) {
        if (selectedArtist != null) {
            val artistTracks = songs.filter { it.artist == selectedArtist }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectedArtist.value = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Text(
                        text = selectedArtist ?: "",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SortHeader(
                    itemCount = artistTracks.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.sortBy.value = it },
                    onToggleDirection = {
                        viewModel.sortDirection.value =
                            if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                            else SortDirection.ASCENDING
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.artistsLayoutMode.value = it },
                    accentColor = dynamicAccent
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (layoutMode) {
                        ViewLayoutMode.LIST_NORMAL -> {
                            LazyColumn(
                                state = detailListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(artistTracks, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, artistTracks) },
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
                                state = detailListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(artistTracks, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, artistTracks) },
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
                                state = detailGridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(artistTracks, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, artistTracks) },
                                        accentColor = dynamicAccent,
                                        isCompactGrid = false
                                    )
                                }
                            }
                        }
                        ViewLayoutMode.GRID_4 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                state = detailGridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(artistTracks, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, artistTracks) },
                                        accentColor = dynamicAccent,
                                        isCompactGrid = true
                                    )
                                }
                            }
                        }
                    }
                    if (layoutMode == ViewLayoutMode.LIST_NORMAL || layoutMode == ViewLayoutMode.LIST_COMPACT) {
                        FloatingScrollbar(
                            listState = detailListState,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            accentColor = dynamicAccent
                        )
                    } else {
                        FloatingScrollbar(
                            gridState = detailGridState,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            accentColor = dynamicAccent
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Artists",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                SortHeader(
                    itemCount = artists.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.sortBy.value = it },
                    onToggleDirection = {
                        viewModel.sortDirection.value =
                            if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                            else SortDirection.ASCENDING
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.artistsLayoutMode.value = it },
                    accentColor = dynamicAccent
                )

                if (artists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No artists found", color = TextMuted, fontSize = 15.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (layoutMode) {
                            ViewLayoutMode.LIST_NORMAL -> {
                                LazyColumn(
                                    state = rootListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(artists, key = { it.name }) { item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(shape = RoundedCornerShape(14.dp), backgroundColor = DarkCardGlass)
                                                .clickable { viewModel.selectedArtist.value = item.name }
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkSurfaceElevated),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = dynamicAccent,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.name,
                                                        color = TextWhite,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.tracks.size} tracks",
                                                        color = TextSecondary,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ViewLayoutMode.LIST_COMPACT -> {
                                LazyColumn(
                                    state = rootListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(artists, key = { it.name }) { item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(shape = RoundedCornerShape(10.dp), backgroundColor = DarkCardGlass)
                                                .clickable { viewModel.selectedArtist.value = item.name }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkSurfaceElevated),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = dynamicAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.name,
                                                        color = TextWhite,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.tracks.size} tracks",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ViewLayoutMode.GRID_3 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    state = rootGridState,
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(artists, key = { it.name }) { item ->
                                        ArtistGridItem(
                                            item = item,
                                            onClick = { viewModel.selectedArtist.value = item.name },
                                            accentColor = dynamicAccent,
                                            isCompactGrid = false
                                        )
                                    }
                                }
                            }
                            ViewLayoutMode.GRID_4 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    state = rootGridState,
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(artists, key = { it.name }) { item ->
                                        ArtistGridItem(
                                            item = item,
                                            onClick = { viewModel.selectedArtist.value = item.name },
                                            accentColor = dynamicAccent,
                                            isCompactGrid = true
                                        )
                                    }
                                }
                            }
                        }
                        if (layoutMode == ViewLayoutMode.LIST_NORMAL || layoutMode == ViewLayoutMode.LIST_COMPACT) {
                            FloatingScrollbar(
                                listState = rootListState,
                                modifier = Modifier.align(Alignment.CenterEnd),
                                accentColor = dynamicAccent
                            )
                        } else {
                            FloatingScrollbar(
                                gridState = rootGridState,
                                modifier = Modifier.align(Alignment.CenterEnd),
                                accentColor = dynamicAccent
                            )
                        }
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

private data class ArtistItem(
    val name: String,
    val tracks: List<Song>
)

@Composable
private fun ArtistGridItem(
    item: ArtistItem,
    onClick: () -> Unit,
    accentColor: Color,
    isCompactGrid: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = DarkCardGlass
            )
            .clickable { onClick() }
            .padding(if (isCompactGrid) 6.dp else 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(if (isCompactGrid) 30.dp else 40.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.name,
                color = TextWhite,
                fontSize = if (isCompactGrid) 10.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${item.tracks.size} tracks",
                color = TextMuted,
                fontSize = if (isCompactGrid) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
