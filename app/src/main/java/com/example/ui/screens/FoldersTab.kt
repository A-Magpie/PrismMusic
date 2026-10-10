package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.FloatingScrollbar
import com.example.ui.components.MultiSelectActionBar
import com.example.ui.components.SongGridItem
import com.example.ui.components.SongItemCard
import com.example.ui.components.SortHeader
import com.example.ui.dialogs.AddToPlaylistBatchDialog
import com.example.ui.dialogs.AddToPlaylistDialog
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
import java.io.File

@Composable
fun FoldersTab(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val songs by viewModel.allSongs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val sortBy by viewModel.foldersSortBy.collectAsState()
    val sortDirection by viewModel.foldersSortDirection.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val layoutMode by viewModel.foldersLayoutMode.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    // Song selection state
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    val isSongSelectionMode by viewModel.isSelectionMode.collectAsState()
    var showBatchAddToPlaylistForSongs by remember { mutableStateOf(false) }
    var singleSongToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Folder selection state
    val selectedFolderPaths by viewModel.selectedFolderPaths.collectAsState()
    val isFolderSelectionMode by viewModel.isFolderSelectionMode.collectAsState()
    var showBatchAddToPlaylistForFolders by remember { mutableStateOf(false) }

    // All unique non-empty normalized directory paths
    val allFolderPaths = remember(songs) {
        songs.map { s -> s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Storage roots detection
    val storageRoots = remember(allFolderPaths) {
        val roots = mutableSetOf<String>()
        for (p in allFolderPaths) {
            when {
                p.startsWith("/storage/emulated/0") -> roots.add("/storage/emulated/0")
                p.startsWith("/storage/") -> {
                    val parts = p.split("/").filter { it.isNotEmpty() }
                    if (parts.size >= 2) {
                        roots.add("/${parts[0]}/${parts[1]}")
                    } else {
                        roots.add(p)
                    }
                }
                p.startsWith("/sdcard") -> roots.add("/sdcard")
                else -> {
                    val parts = p.split("/").filter { it.isNotEmpty() }
                    if (parts.isNotEmpty()) {
                        roots.add("/${parts[0]}")
                    }
                }
            }
        }
        roots.toList().sorted()
    }

    val initialRoot = remember(storageRoots) {
        if (storageRoots.size == 1) storageRoots.first() else null
    }

    var currentDirectoryPath by remember(initialRoot) {
        mutableStateOf(initialRoot)
    }

    // Hardware & Gesture Back Handler
    BackHandler(enabled = isFolderSelectionMode) {
        viewModel.clearFolderSelection()
    }
    BackHandler(enabled = !isFolderSelectionMode && isSongSelectionMode) {
        viewModel.clearSelection()
    }
    BackHandler(enabled = !isFolderSelectionMode && !isSongSelectionMode && currentDirectoryPath != null) {
        val cur = currentDirectoryPath!!
        val root = storageRoots.firstOrNull()
        if (root != null && cur == root) {
            if (storageRoots.size > 1) {
                currentDirectoryPath = null
            }
        } else {
            val parent = File(cur).parent
            if (parent != null && root != null && parent.length >= root.length) {
                currentDirectoryPath = parent
            } else if (storageRoots.size > 1) {
                currentDirectoryPath = null
            } else {
                currentDirectoryPath = root
            }
        }
    }

    // Compute Directory Contents
    val activePath = currentDirectoryPath

    // If currentDirectoryPath is null and multiple storage roots exist: show storage roots
    val isShowingStorageRoots = activePath == null && storageRoots.size > 1

    val subdirectories = remember(songs, allFolderPaths, activePath, isShowingStorageRoots, storageRoots, sortBy, sortDirection) {
        if (isShowingStorageRoots) {
            storageRoots.map { rootPath ->
                val name = if (rootPath == "/storage/emulated/0" || rootPath == "/sdcard") "Internal Storage"
                else "SD Card (${rootPath.substringAfterLast('/')})"
                val rootSongs = songs.filter { s ->
                    val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                    p == rootPath || p.startsWith("$rootPath/")
                }
                SubfolderItem(
                    path = rootPath,
                    name = name,
                    directSongCount = rootSongs.count { it.folderPath.ifBlank { File(it.path).parent ?: "" }.trimEnd('/') == rootPath },
                    totalSongCount = rootSongs.size,
                    tracks = rootSongs
                )
            }
        } else if (activePath != null) {
            val prefix = activePath.trimEnd('/') + "/"
            val descendantFolders = allFolderPaths.filter { it.startsWith(prefix) }
            val immediatePaths = descendantFolders.map { fullPath ->
                val relative = fullPath.removePrefix(prefix)
                val childName = relative.substringBefore('/')
                "$activePath/$childName"
            }.distinct()

            val list = immediatePaths.map { childPath ->
                val childName = childPath.substringAfterLast('/')
                val childPrefix = "$childPath/"
                val childSongs = songs.filter { s ->
                    val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                    p == childPath || p.startsWith(childPrefix)
                }
                val directCount = childSongs.count { s ->
                    val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                    p == childPath
                }
                SubfolderItem(
                    path = childPath,
                    name = childName,
                    directSongCount = directCount,
                    totalSongCount = childSongs.size,
                    tracks = childSongs
                )
            }

            val sorted = when (sortBy) {
                SortBy.TITLE -> list.sortedBy { it.name.lowercase() }
                SortBy.DATE_MODIFIED -> list.sortedBy { it.tracks.maxOfOrNull { s -> s.dateModified } ?: 0L }
                SortBy.DATE_ADDED -> list.sortedBy { it.tracks.maxOfOrNull { s -> s.dateAdded } ?: 0L }
                SortBy.DURATION -> list.sortedBy { it.tracks.sumOf { s -> s.duration } }
                SortBy.PLAY_COUNT -> list.sortedBy { it.tracks.sumOf { s -> s.playCount } }
                SortBy.CUSTOM -> list
            }
            if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
        } else {
            emptyList()
        }
    }

    // Direct songs in active directory
    val directSongs = remember(songs, activePath, isShowingStorageRoots, sortBy, sortDirection) {
        if (isShowingStorageRoots || activePath == null) {
            emptyList()
        } else {
            val raw = songs.filter { s ->
                val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                p == activePath
            }
            val sorted = when (sortBy) {
                SortBy.TITLE -> raw.sortedBy { it.title.lowercase() }
                SortBy.DATE_MODIFIED -> raw.sortedBy { it.dateModified }
                SortBy.DATE_ADDED -> raw.sortedBy { it.dateAdded }
                SortBy.DURATION -> raw.sortedBy { it.duration }
                SortBy.PLAY_COUNT -> raw.sortedBy { it.playCount }
                SortBy.CUSTOM -> raw
            }
            if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
        }
    }

    // All songs in this folder and its subdirectories (for Play All)
    val allFolderSongs = remember(songs, activePath, isShowingStorageRoots) {
        if (isShowingStorageRoots || activePath == null) {
            songs
        } else {
            val prefix = activePath.trimEnd('/') + "/"
            songs.filter { s ->
                val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                p == activePath || p.startsWith(prefix)
            }
        }
    }

    // Breadcrumb parts
    val breadcrumbs = remember(activePath, storageRoots) {
        if (activePath == null) {
            listOf(BreadcrumbItem("Storages", null))
        } else {
            val list = mutableListOf<BreadcrumbItem>()
            val matchingRoot = storageRoots.find { activePath.startsWith(it) } ?: storageRoots.firstOrNull()
            if (matchingRoot != null) {
                val rootLabel = if (matchingRoot == "/storage/emulated/0" || matchingRoot == "/sdcard") "Internal Storage"
                else "SD Card (${matchingRoot.substringAfterLast('/')})"
                list.add(BreadcrumbItem(rootLabel, matchingRoot))

                val remainder = activePath.removePrefix(matchingRoot).trim('/')
                if (remainder.isNotEmpty()) {
                    var accumulated = matchingRoot
                    val segments = remainder.split('/')
                    for (seg in segments) {
                        accumulated = "$accumulated/$seg"
                        list.add(BreadcrumbItem(seg, accumulated))
                    }
                }
            } else {
                list.add(BreadcrumbItem(File(activePath).name.ifBlank { "Storage" }, activePath))
            }
            list
        }
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 14.dp)
            .testTag("folders_tab")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back, Title, and Scan button
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
                    val canGoBack = activePath != null && (storageRoots.size > 1 || activePath != storageRoots.firstOrNull())
                    if (canGoBack) {
                        IconButton(onClick = {
                            val cur = activePath!!
                            val root = storageRoots.firstOrNull()
                            if (root != null && cur == root) {
                                if (storageRoots.size > 1) {
                                    currentDirectoryPath = null
                                }
                            } else {
                                val parent = File(cur).parent
                                if (parent != null && root != null && parent.length >= root.length) {
                                    currentDirectoryPath = parent
                                } else if (storageRoots.size > 1) {
                                    currentDirectoryPath = null
                                } else {
                                    currentDirectoryPath = root
                                }
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    }

                    val titleText = when {
                        isShowingStorageRoots -> "Storage Devices"
                        activePath == "/storage/emulated/0" || activePath == "/sdcard" -> "Internal Storage"
                        activePath != null -> File(activePath).name.ifBlank { activePath }
                        else -> "Music Folders"
                    }
                    Text(
                        text = titleText,
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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

            // Breadcrumb Navigation Bar
            if (breadcrumbs.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(breadcrumbs) { idx, crumb ->
                        val isLast = idx == breadcrumbs.size - 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLast) dynamicAccent.copy(alpha = 0.2f) else DarkCardGlass)
                                .clickable(enabled = !isLast) {
                                    currentDirectoryPath = crumb.path
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = crumb.name,
                                color = if (isLast) dynamicAccent else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Play All Button (plays all songs in current directory tree)
            if (allFolderSongs.isNotEmpty() && !isShowingStorageRoots) {
                Button(
                    onClick = { viewModel.playSong(allFolderSongs.first(), allFolderSongs) },
                    colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Play Folder (${allFolderSongs.size} tracks)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sort & Layout Header
            SortHeader(
                itemCount = subdirectories.size + directSongs.size,
                sortBy = sortBy,
                sortDirection = sortDirection,
                onSortByChanged = { viewModel.setTabSort("folders", newSortBy = it) },
                onToggleDirection = {
                    viewModel.setTabSort(
                        "folders",
                        newSortDir = if (sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                        else SortDirection.ASCENDING
                    )
                },
                layoutMode = layoutMode,
                onLayoutModeChanged = { viewModel.setLayoutMode("folders", it) },
                accentColor = dynamicAccent
            )

            if (subdirectories.isEmpty() && directSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audio files found in this folder", color = TextMuted, fontSize = 15.sp)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (layoutMode) {
                        ViewLayoutMode.LIST_NORMAL, ViewLayoutMode.LIST_COMPACT -> {
                            val isCompact = layoutMode == ViewLayoutMode.LIST_COMPACT
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                // Subfolders Section
                                if (subdirectories.isNotEmpty()) {
                                    if (directSongs.isNotEmpty()) {
                                        item(key = "section_folders") {
                                            Text(
                                                text = "Folders (${subdirectories.size})",
                                                color = dynamicAccent,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                            )
                                        }
                                    }
                                    items(subdirectories, key = { "folder_${it.path}" }) { item ->
                                        val isSelected = item.path in selectedFolderPaths
                                        FolderListItem(
                                            item = item,
                                            isSelected = isSelected,
                                            isSelectionMode = isFolderSelectionMode,
                                            onClick = {
                                                if (isFolderSelectionMode) {
                                                    viewModel.toggleFolderSelection(item.path)
                                                } else {
                                                    currentDirectoryPath = item.path
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleFolderSelection(item.path)
                                            },
                                            accentColor = dynamicAccent,
                                            isCompact = isCompact
                                        )
                                    }
                                }

                                // Direct Songs Section
                                if (directSongs.isNotEmpty()) {
                                    if (subdirectories.isNotEmpty()) {
                                        item(key = "section_tracks") {
                                            Text(
                                                text = "Tracks (${directSongs.size})",
                                                color = dynamicAccent,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                            )
                                        }
                                    }
                                    items(directSongs, key = { "song_${it.id}" }) { song ->
                                        SongItemCard(
                                            song = song,
                                            isPlaying = isPlaying,
                                            isCurrent = (currentSong?.id == song.id),
                                            onClick = { viewModel.playSong(song, directSongs) },
                                            onPlayNext = { viewModel.playNextInQueue(song) },
                                            onAddToQueue = { viewModel.addToQueueEnd(song) },
                                            onAddToPlaylist = { singleSongToAddToPlaylist = song },
                                            onEditTags = { viewModel.songForTagEditor.value = song },
                                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                                            onDelete = { viewModel.deleteSong(song) },
                                            accentColor = dynamicAccent,
                                            isCompact = isCompact,
                                            isSelectionMode = isSongSelectionMode,
                                            isSelected = selectedSongIds.contains(song.id),
                                            onLongClick = { viewModel.toggleSongSelection(song.id) }
                                        )
                                    }
                                }
                            }
                        }

                        ViewLayoutMode.GRID_3, ViewLayoutMode.GRID_4 -> {
                            val columnsCount = if (layoutMode == ViewLayoutMode.GRID_3) 3 else 4
                            val isCompactGrid = layoutMode == ViewLayoutMode.GRID_4
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnsCount),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactGrid) 6.dp else 8.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactGrid) 6.dp else 8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(subdirectories, key = { "folder_${it.path}" }) { item ->
                                    val isSelected = item.path in selectedFolderPaths
                                    FolderGridItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = isFolderSelectionMode,
                                        onClick = {
                                            if (isFolderSelectionMode) {
                                                viewModel.toggleFolderSelection(item.path)
                                            } else {
                                                currentDirectoryPath = item.path
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleFolderSelection(item.path)
                                        },
                                        accentColor = dynamicAccent,
                                        isCompactGrid = isCompactGrid
                                    )
                                }
                                items(directSongs, key = { "song_${it.id}" }) { song ->
                                    SongGridItem(
                                        song = song,
                                        isPlaying = isPlaying,
                                        isCurrent = (currentSong?.id == song.id),
                                        onClick = { viewModel.playSong(song, directSongs) },
                                        accentColor = dynamicAccent,
                                        isCompactGrid = isCompactGrid
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

                    // Folder Multi-Select Action Bar
                    FolderMultiSelectActionBar(
                        selectedCount = selectedFolderPaths.size,
                        totalCount = subdirectories.size,
                        accentColor = dynamicAccent,
                        onSelectAll = { viewModel.selectAllFolders(subdirectories.map { it.path }) },
                        onClose = { viewModel.clearFolderSelection() },
                        onPlayNow = { viewModel.playSelectedFoldersNow(selectedFolderPaths, songs) },
                        onPlayNext = { viewModel.playSelectedFoldersNext(selectedFolderPaths, songs) },
                        onAddToQueue = { viewModel.addSelectedFoldersToQueue(selectedFolderPaths, songs) },
                        onAddToPlaylist = { showBatchAddToPlaylistForFolders = true },
                        onDelete = { viewModel.deleteSelectedFolders(context, selectedFolderPaths, songs) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 75.dp)
                    )

                    // Song Multi-Select Action Bar
                    MultiSelectActionBar(
                        selectedCount = selectedSongIds.size,
                        totalCount = directSongs.size,
                        accentColor = dynamicAccent,
                        onSelectAll = { viewModel.selectAllSongs(directSongs) },
                        onClose = { viewModel.clearSelection() },
                        onPlayNow = { viewModel.playSelectedSongsNow(directSongs) },
                        onPlayNext = { viewModel.playSelectedSongsNext(directSongs) },
                        onAddToQueue = { viewModel.addSelectedSongsToQueue(directSongs) },
                        onAddToPlaylist = { showBatchAddToPlaylistForSongs = true },
                        onDelete = { viewModel.deleteSelectedSongs(context, directSongs) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 75.dp)
                    )
                }
            }
        }

        // Single Song Add to Playlist Dialog
        singleSongToAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { singleSongToAddToPlaylist = null }
            )
        }

        // Batch Add Songs to Playlist Dialog
        if (showBatchAddToPlaylistForSongs) {
            val selectedSongs = songs.filter { it.id in selectedSongIds }
            AddToPlaylistBatchDialog(
                songs = selectedSongs,
                viewModel = viewModel,
                onDismiss = { showBatchAddToPlaylistForSongs = false }
            )
        }

        // Batch Add Folders' Songs to Playlist Dialog
        if (showBatchAddToPlaylistForFolders) {
            val songsInSelectedFolders = songs.filter { s ->
                val p = s.folderPath.ifBlank { File(s.path).parent ?: "" }.trimEnd('/')
                selectedFolderPaths.any { fp -> p == fp || p.startsWith("$fp/") }
            }
            AddToPlaylistBatchDialog(
                songs = songsInSelectedFolders,
                viewModel = viewModel,
                onDismiss = {
                    showBatchAddToPlaylistForFolders = false
                    viewModel.clearFolderSelection()
                }
            )
        }
    }
}

private data class BreadcrumbItem(
    val name: String,
    val path: String?
)

private data class SubfolderItem(
    val path: String,
    val name: String,
    val directSongCount: Int,
    val totalSongCount: Int,
    val tracks: List<Song>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderListItem(
    item: SubfolderItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                backgroundColor = if (isSelected) accentColor.copy(alpha = 0.25f) else DarkCardGlass
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(if (isCompact) PaddingValues(horizontal = 12.dp, vertical = 8.dp) else PaddingValues(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
        ) {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 20.dp else 24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor else Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(if (isCompact) 12.dp else 15.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(if (isCompact) 26.dp else 36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = TextWhite,
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.totalSongCount} tracks",
                    color = TextSecondary,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderGridItem(
    item: SubfolderItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color,
    isCompactGrid: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(12.dp),
                backgroundColor = if (isSelected) accentColor.copy(alpha = 0.25f) else DarkCardGlass
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopEnd)
                            .size(if (isCompactGrid) 18.dp else 22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(if (isCompactGrid) 12.dp else 14.dp)
                            )
                        }
                    }
                }
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
                text = "${item.totalSongCount} tracks",
                color = TextMuted,
                fontSize = if (isCompactGrid) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FolderMultiSelectActionBar(
    selectedCount: Int,
    totalCount: Int,
    accentColor: Color,
    onSelectAll: () -> Unit,
    onClose: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
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
                        onClick = onClose,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$selectedCount Selected",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                TextButton(onClick = onSelectAll) {
                    Text(
                        text = if (selectedCount == totalCount) "Deselect All" else "Select All ($totalCount)",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onPlayNow,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = onPlayNext,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Next", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = onAddToQueue,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Queue", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = onAddToPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Playlist", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { showBatchDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Delete", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        if (showBatchDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                containerColor = Color(0xFF1E1E24),
                title = { Text("Delete $selectedCount Folder(s)?", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to permanently delete all tracks in the selected folder(s) from storage?", color = TextMuted)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatchDeleteConfirm = false
                            onDelete()
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
