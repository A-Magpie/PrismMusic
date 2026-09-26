package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.FloatingScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.activity.compose.BackHandler
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
import com.example.ui.theme.AccentCyan
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
import java.io.File

@Composable
fun FoldersTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allSongs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val layoutMode by viewModel.foldersLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Hardware and Gesture Back Handler: Return to folder list instead of switching tabs or closing app
    BackHandler(enabled = selectedFolder != null) {
        viewModel.selectedFolder.value = null
    }

    // Group songs by folder
    val folders = remember(songs, sortBy, sortDirection) {
        val grouped = songs.groupBy { it.folderPath.ifBlank { "Root / Unknown" } }
        val folderList = grouped.map { (path, trackList) ->
            val name = File(path).name.ifBlank { path }
            FolderItem(path = path, name = name, tracks = trackList)
        }
        val sorted = when (sortBy) {
            com.example.ui.viewmodel.SortBy.TITLE -> folderList.sortedBy { it.name.lowercase() }
            com.example.ui.viewmodel.SortBy.DATE_MODIFIED -> folderList.sortedBy { it.tracks.maxOfOrNull { s -> s.dateModified } ?: 0L }
            com.example.ui.viewmodel.SortBy.DATE_ADDED -> folderList.sortedBy { it.tracks.maxOfOrNull { s -> s.dateAdded } ?: 0L }
            com.example.ui.viewmodel.SortBy.DURATION -> folderList.sortedBy { it.tracks.sumOf { s -> s.duration } }
            com.example.ui.viewmodel.SortBy.PLAY_COUNT -> folderList.sortedBy { it.tracks.sumOf { s -> s.playCount } }
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
            .testTag("folders_tab")
    ) {
        if (selectedFolder != null) {
            // Folder Detail view
            val folderSongs = songs.filter { it.folderPath == selectedFolder || (selectedFolder == "Root / Unknown" && it.folderPath.isBlank()) }
            val folderName = File(selectedFolder ?: "").name.ifBlank { selectedFolder ?: "" }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectedFolder.value = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Text(
                        text = folderName,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SortHeader(
                    itemCount = folderSongs.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.sortBy.value = it },
                    onToggleDirection = {
                        viewModel.sortDirection.value =
                            if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                            else SortDirection.ASCENDING
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.foldersLayoutMode.value = it },
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
                                items(folderSongs, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, folderSongs) },
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
                                items(folderSongs, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, folderSongs) },
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
                                items(folderSongs, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, folderSongs) },
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
                                items(folderSongs, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, folderSongs) },
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
            // Folders List view
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music Folders",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isScanning) {
                        CircularProgressIndicator(
                            color = dynamicAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshMedia() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Folders", tint = dynamicAccent)
                        }
                    }
                }

                SortHeader(
                    itemCount = folders.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.sortBy.value = it },
                    onToggleDirection = {
                        viewModel.sortDirection.value =
                            if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                            else SortDirection.ASCENDING
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.foldersLayoutMode.value = it },
                    accentColor = dynamicAccent
                )

                if (folders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No folders found", color = TextMuted, fontSize = 15.sp)
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
                                    items(folders, key = { it.path }) { item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(shape = RoundedCornerShape(14.dp), backgroundColor = DarkCardGlass)
                                                .clickable { viewModel.selectedFolder.value = item.path }
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = dynamicAccent,
                                                    modifier = Modifier.size(36.dp)
                                                )
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
                                                        text = "${item.tracks.size} tracks • ${item.path}",
                                                        color = TextSecondary,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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
                                    items(folders, key = { it.path }) { item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(shape = RoundedCornerShape(10.dp), backgroundColor = DarkCardGlass)
                                                .clickable { viewModel.selectedFolder.value = item.path }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = dynamicAccent,
                                                    modifier = Modifier.size(24.dp)
                                                )
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
                                                        text = "${item.tracks.size} tracks • ${item.path}",
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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
                                    items(folders, key = { it.path }) { item ->
                                        FolderGridItem(
                                            item = item,
                                            onClick = { viewModel.selectedFolder.value = item.path },
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
                                    items(folders, key = { it.path }) { item ->
                                        FolderGridItem(
                                            item = item,
                                            onClick = { viewModel.selectedFolder.value = item.path },
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

private data class FolderItem(
    val path: String,
    val name: String,
    val tracks: List<Song>
)

@Composable
private fun FolderGridItem(
    item: FolderItem,
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(if (isCompactGrid) 32.dp else 44.dp)
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
