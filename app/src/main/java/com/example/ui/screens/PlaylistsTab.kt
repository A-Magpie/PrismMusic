package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewModule
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
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.components.SquareCoverArt
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.SortBy
import com.example.ui.viewmodel.SortDirection
import com.example.ui.viewmodel.ViewLayoutMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.allPlaylists.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.playlistsSortBy.collectAsState()
    val sortDirection by viewModel.playlistsSortDirection.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val gridColumns by viewModel.playlistGridColumns.collectAsState()
    val layoutMode by viewModel.playlistsLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()
    val m3uImportMessage by viewModel.m3uImportMessage.collectAsState()
    val selectedPlaylistIds by viewModel.selectedPlaylistIds.collectAsState()
    val isPlaylistSelectionMode by viewModel.isPlaylistSelectionMode.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddToAnotherPlaylist by remember { mutableStateOf<Song?>(null) }

    // Hardware and Gesture Back Handler: Clear selection or return to playlist list
    BackHandler(enabled = isPlaylistSelectionMode) {
        viewModel.clearPlaylistSelection()
    }
    BackHandler(enabled = !isPlaylistSelectionMode && selectedPlaylist != null) {
        viewModel.selectedPlaylist.value = null
    }

    // Edit Playlist Dialog State
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }
    var editPlaylistCoverUri by remember { mutableStateOf<String?>(null) }

    // Cover image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            editPlaylistCoverUri = uri.toString()
        }
    }

    // M3U file picker
    val m3uPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importM3uPlaylist(uri)
        }
    }

    val sortedPlaylists = remember(playlists, sortBy, sortDirection) {
        val sorted = when (sortBy) {
            SortBy.TITLE -> playlists.sortedBy { it.name.lowercase() }
            SortBy.DATE_MODIFIED -> playlists.sortedBy { it.createdAt }
            SortBy.DATE_ADDED -> playlists.sortedBy { it.createdAt }
            SortBy.DURATION -> playlists.sortedBy { it.id }
            SortBy.PLAY_COUNT -> playlists.sortedBy { it.name }
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
            .testTag("playlists_tab")
    ) {
        if (selectedPlaylist != null) {
            val playlist = selectedPlaylist!!
            val playlistSongsFlow = remember(playlist.id) { viewModel.getSongsForPlaylist(playlist.id) }
            val rawSongsInPlaylist by playlistSongsFlow.collectAsState(initial = emptyList())
            val songsInPlaylist = remember(rawSongsInPlaylist, sortBy, sortDirection) {
                val s = when (sortBy) {
                    SortBy.TITLE -> rawSongsInPlaylist.sortedBy { it.title.lowercase() }
                    SortBy.DATE_MODIFIED -> rawSongsInPlaylist.sortedBy { it.dateModified }
                    SortBy.DATE_ADDED -> rawSongsInPlaylist.sortedBy { it.dateAdded }
                    SortBy.DURATION -> rawSongsInPlaylist.sortedBy { it.duration }
                    SortBy.PLAY_COUNT -> rawSongsInPlaylist.sortedBy { it.playCount }
                }
                if (sortDirection == SortDirection.DESCENDING) s.reversed() else s
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header with back, title, and actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = { viewModel.selectedPlaylist.value = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                        Text(
                            text = playlist.name,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            editingPlaylist = playlist
                            editPlaylistName = playlist.name
                            editPlaylistCoverUri = playlist.customCoverUri
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Playlist", tint = dynamicAccent)
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = AccentRed)
                        }
                    }
                }

                if (songsInPlaylist.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.playSong(songsInPlaylist.first(), songsInPlaylist) },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Play Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                SortHeader(
                    itemCount = songsInPlaylist.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.setTabSort("playlists", newSortBy = it) },
                    onToggleDirection = {
                        val newDir = if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                        viewModel.setTabSort("playlists", newSortDir = newDir)
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.setLayoutMode("playlists", it) },
                    accentColor = dynamicAccent
                )

                if (songsInPlaylist.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No songs in this playlist yet", color = TextMuted, fontSize = 15.sp)
                    }
                } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (layoutMode) {
                        ViewLayoutMode.LIST_NORMAL -> {
                            LazyColumn(
                                state = detailListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(songsInPlaylist, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, songsInPlaylist) },
                                        onPlayNext = { viewModel.playNextInQueue(song) },
                                        onAddToQueue = { viewModel.addToQueueEnd(song) },
                                        onAddToPlaylist = { songToAddToAnotherPlaylist = song },
                                        onEditTags = { viewModel.songForTagEditor.value = song },
                                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                                        onDelete = { viewModel.removeSongFromPlaylist(playlist.id, song) },
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
                                items(songsInPlaylist, key = { it.id }) { song ->
                                    SongItemCard(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, songsInPlaylist) },
                                        onPlayNext = { viewModel.playNextInQueue(song) },
                                        onAddToQueue = { viewModel.addToQueueEnd(song) },
                                        onAddToPlaylist = { songToAddToAnotherPlaylist = song },
                                        onEditTags = { viewModel.songForTagEditor.value = song },
                                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                                        onDelete = { viewModel.removeSongFromPlaylist(playlist.id, song) },
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
                                items(songsInPlaylist, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, songsInPlaylist) },
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
                                items(songsInPlaylist, key = { it.id }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, songsInPlaylist) },
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
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title, Import M3U, 2/3 column toggle, and + Add
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlists",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Auto-Scan Device Playlists Button
                        IconButton(
                            onClick = { viewModel.autoScanAndImportAllPlaylists() },
                            modifier = Modifier.testTag("btn_auto_scan_playlists")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Auto-Scan Playlists",
                                tint = dynamicAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Import M3U Button
                        IconButton(
                            onClick = { m3uPickerLauncher.launch("*/*") },
                            modifier = Modifier.testTag("btn_import_m3u")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Import M3U",
                                tint = dynamicAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Toggle 2 vs 3 Cards per row
                        IconButton(
                            onClick = {
                                viewModel.playlistGridColumns.value = if (gridColumns == 2) 3 else 2
                            },
                            modifier = Modifier.testTag("btn_toggle_playlist_columns")
                        ) {
                            Icon(
                                imageVector = if (gridColumns == 2) Icons.Default.ViewModule else Icons.Default.GridView,
                                contentDescription = "Toggle Grid Columns",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Create Playlist Button
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.testTag("btn_create_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = dynamicAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                SortHeader(
                    itemCount = sortedPlaylists.size,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    onSortByChanged = { viewModel.setTabSort("playlists", newSortBy = it) },
                    onToggleDirection = {
                        val newDir = if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                        viewModel.setTabSort("playlists", newSortDir = newDir)
                    },
                    layoutMode = layoutMode,
                    onLayoutModeChanged = { viewModel.setLayoutMode("playlists", it) },
                    accentColor = dynamicAccent
                )

                if (sortedPlaylists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No playlists found.\nTap + to create one, or auto-scan your storage for playlists.",
                                color = TextMuted,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.autoScanAndImportAllPlaylists() },
                                colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Auto-Scan Device Playlists", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
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
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        val isSelected = playlist.id in selectedPlaylistIds
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(
                                                    shape = RoundedCornerShape(14.dp),
                                                    backgroundColor = if (isSelected) dynamicAccent.copy(alpha = 0.22f) else DarkCardGlass
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isPlaylistSelectionMode) {
                                                            viewModel.togglePlaylistSelection(playlist.id)
                                                        } else {
                                                            viewModel.selectedPlaylist.value = playlist
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePlaylistSelection(playlist.id)
                                                    }
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                if (isPlaylistSelectionMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) dynamicAccent else Color(0x33FFFFFF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                SquareCoverArt(
                                                    albumArtUri = playlist.customCoverUri,
                                                    contentDescription = playlist.name,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.size(54.dp)
                                                )
                                                Text(
                                                    text = playlist.name,
                                                    color = TextWhite,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (!isPlaylistSelectionMode) {
                                                    IconButton(
                                                        onClick = {
                                                            editingPlaylist = playlist
                                                            editPlaylistName = playlist.name
                                                            editPlaylistCoverUri = playlist.customCoverUri
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Playlist",
                                                            tint = dynamicAccent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deletePlaylist(playlist) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Playlist",
                                                            tint = AccentRed,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
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
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        val isSelected = playlist.id in selectedPlaylistIds
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(
                                                    shape = RoundedCornerShape(10.dp),
                                                    backgroundColor = if (isSelected) dynamicAccent.copy(alpha = 0.22f) else DarkCardGlass
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isPlaylistSelectionMode) {
                                                            viewModel.togglePlaylistSelection(playlist.id)
                                                        } else {
                                                            viewModel.selectedPlaylist.value = playlist
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePlaylistSelection(playlist.id)
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (isPlaylistSelectionMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) dynamicAccent else Color(0x33FFFFFF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                SquareCoverArt(
                                                    albumArtUri = playlist.customCoverUri,
                                                    contentDescription = playlist.name,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.size(38.dp)
                                                )
                                                Text(
                                                    text = playlist.name,
                                                    color = TextWhite,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (!isPlaylistSelectionMode) {
                                                    IconButton(
                                                        onClick = {
                                                            editingPlaylist = playlist
                                                            editPlaylistName = playlist.name
                                                            editPlaylistCoverUri = playlist.customCoverUri
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Playlist",
                                                            tint = dynamicAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
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
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        val isSelected = playlist.id in selectedPlaylistIds
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(
                                                    shape = RoundedCornerShape(12.dp),
                                                    backgroundColor = if (isSelected) dynamicAccent.copy(alpha = 0.22f) else DarkCardGlass
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isPlaylistSelectionMode) {
                                                            viewModel.togglePlaylistSelection(playlist.id)
                                                        } else {
                                                            viewModel.selectedPlaylist.value = playlist
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePlaylistSelection(playlist.id)
                                                    }
                                                )
                                                .padding(6.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                                    SquareCoverArt(
                                                        albumArtUri = playlist.customCoverUri,
                                                        contentDescription = playlist.name,
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (isPlaylistSelectionMode) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .align(Alignment.TopEnd)
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) dynamicAccent else Color(0x88000000)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = playlist.name,
                                                    color = TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
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
                                    items(sortedPlaylists, key = { it.id }) { playlist ->
                                        val isSelected = playlist.id in selectedPlaylistIds
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassmorphic(
                                                    shape = RoundedCornerShape(10.dp),
                                                    backgroundColor = if (isSelected) dynamicAccent.copy(alpha = 0.22f) else DarkCardGlass
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isPlaylistSelectionMode) {
                                                            viewModel.togglePlaylistSelection(playlist.id)
                                                        } else {
                                                            viewModel.selectedPlaylist.value = playlist
                                                        }
                                                    },
                                                    onLongClick = {
                                                        viewModel.togglePlaylistSelection(playlist.id)
                                                    }
                                                )
                                                .padding(4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                                    SquareCoverArt(
                                                        albumArtUri = playlist.customCoverUri,
                                                        contentDescription = playlist.name,
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (isPlaylistSelectionMode) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(2.dp)
                                                                .align(Alignment.TopEnd)
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) dynamicAccent else Color(0x88000000)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = playlist.name,
                                                    color = TextWhite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
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

                        // Multi-Select Floating Action Bar for Playlists
                        AnimatedVisibility(
                            visible = isPlaylistSelectionMode && selectedPlaylistIds.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 75.dp)
                        ) {
                            var showBatchDeleteConfirm by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF16161A))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.clearPlaylistSelection() },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedPlaylistIds.size} Selected",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    TextButton(onClick = { viewModel.selectAllPlaylists(sortedPlaylists) }) {
                                        Text(
                                            text = if (selectedPlaylistIds.size == sortedPlaylists.size) "Deselect All" else "Select All (${sortedPlaylists.size})",
                                            color = dynamicAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.playSelectedPlaylistsNow(sortedPlaylists) },
                                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showBatchDeleteConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            if (showBatchDeleteConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showBatchDeleteConfirm = false },
                                    containerColor = Color(0xFF1E1E24),
                                    title = { Text("Delete ${selectedPlaylistIds.size} Playlist(s)?", color = TextWhite, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Text("Are you sure you want to delete the selected playlist(s)? Tracks will not be removed from storage.", color = TextMuted)
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showBatchDeleteConfirm = false
                                                viewModel.deleteSelectedPlaylists(sortedPlaylists)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                                        ) {
                                            Text("Delete", color = TextWhite, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showBatchDeleteConfirm = false }) {
                                            Text("Cancel", color = TextMuted)
                                        }
                                    }
                                )
                            }
                        }
                }
                }
            }
        }

        // Create Playlist Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = DarkCardGlass,
                title = { Text("New Playlist", color = TextWhite) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = dynamicAccent,
                            unfocusedBorderColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName.trim())
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                    ) {
                        Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }

        // Edit Playlist Dialog (Rename & Custom Cover Art)
        if (editingPlaylist != null) {
            val pl = editingPlaylist!!
            AlertDialog(
                onDismissRequest = { editingPlaylist = null },
                containerColor = DarkCardGlass,
                title = { Text("Edit Playlist", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cover preview with change button
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            SquareCoverArt(
                                albumArtUri = editPlaylistCoverUri,
                                contentDescription = "Playlist Cover",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Change Cover",
                                        tint = TextWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Change Cover", color = TextWhite, fontSize = 10.sp)
                                }
                            }
                        }

                        // Playlist name field
                        OutlinedTextField(
                            value = editPlaylistName,
                            onValueChange = { editPlaylistName = it },
                            label = { Text("Playlist Name", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = dynamicAccent,
                                unfocusedBorderColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editPlaylistName.isNotBlank()) {
                                viewModel.updatePlaylistDetails(
                                    pl.id,
                                    editPlaylistName.trim(),
                                    editPlaylistCoverUri
                                )
                                editingPlaylist = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPlaylist = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }

        // M3U Import Result Dialog
        if (m3uImportMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.m3uImportMessage.value = null },
                containerColor = DarkCardGlass,
                title = { Text("Import Playlist", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = { Text(m3uImportMessage ?: "", color = TextWhite, fontSize = 14.sp) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.m3uImportMessage.value = null },
                        colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        songToAddToAnotherPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { songToAddToAnotherPlaylist = null }
            )
        }
    }
}
