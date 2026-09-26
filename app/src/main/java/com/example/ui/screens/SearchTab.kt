package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SongItemCard
import com.example.ui.components.SquareCoverArt
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.glassmorphic
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun SearchTab(
    viewModel: MusicViewModel,
    isTabBarAtTop: Boolean,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showAllSongs by remember(query) { mutableStateOf(false) }
    var showAllArtists by remember(query) { mutableStateOf(false) }
    var showAllAlbums by remember(query) { mutableStateOf(false) }

    // Categorized matches
    val matchingSongs = remember(allSongs, query) {
        if (query.isBlank()) emptyList()
        else allSongs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    val matchingArtists = remember(allSongs, query) {
        if (query.isBlank()) emptyList()
        else allSongs.map { it.artist }.filter { it.isNotBlank() && it.contains(query, ignoreCase = true) }.distinct()
    }

    val matchingAlbums = remember(allSongs, query) {
        if (query.isBlank()) emptyList()
        else allSongs.filter { it.album.isNotBlank() && it.album.contains(query, ignoreCase = true) }
            .groupBy { it.album }
            .map { (album, tracks) -> SearchAlbumItem(name = album, artist = tracks.firstOrNull()?.artist ?: "Unknown Artist", artUri = tracks.firstOrNull()?.albumArtUri, tracks = tracks) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 14.dp)
            .testTag("search_tab")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top area: Results list rendered from bottom up
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (query.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Search tracks, artists, and albums",
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else if (matchingSongs.isEmpty() && matchingArtists.isEmpty() && matchingAlbums.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No matches found for \"$query\"",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section 3 (Top): Albums (if any)
                        if (matchingAlbums.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Albums (${matchingAlbums.size})",
                                        color = dynamicAccent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val visibleAlbums = if (showAllAlbums) matchingAlbums else matchingAlbums.take(3)
                            items(visibleAlbums, key = { it.name }) { album ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassmorphic(shape = RoundedCornerShape(12.dp), backgroundColor = DarkCardGlass)
                                        .clickable {
                                            if (album.tracks.isNotEmpty()) {
                                                viewModel.playSong(album.tracks.first(), album.tracks)
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SquareCoverArt(
                                            albumArtUri = album.artUri,
                                            contentDescription = album.name,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.name,
                                                color = TextWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${album.artist} • ${album.tracks.size} tracks",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                }
                            }

                            if (matchingAlbums.size > 3) {
                                item {
                                    OutlinedButton(
                                        onClick = { showAllAlbums = !showAllAlbums },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dynamicAccent),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showAllAlbums) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (showAllAlbums) "Show Less Albums" else "More Albums (${matchingAlbums.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Section 2 (Middle): Artists (if any)
                        if (matchingArtists.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Artists (${matchingArtists.size})",
                                        color = dynamicAccent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val visibleArtists = if (showAllArtists) matchingArtists else matchingArtists.take(3)
                            items(visibleArtists, key = { it }) { artistName ->
                                val artistTracks = allSongs.filter { it.artist == artistName }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassmorphic(shape = RoundedCornerShape(12.dp), backgroundColor = DarkCardGlass)
                                        .clickable {
                                            viewModel.selectedArtist.value = artistName
                                            viewModel.selectedTab.value = com.example.ui.viewmodel.TabType.ARTISTS
                                        }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(dynamicAccent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = dynamicAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artistName,
                                                color = TextWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${artistTracks.size} tracks",
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                }
                            }

                            if (matchingArtists.size > 3) {
                                item {
                                    OutlinedButton(
                                        onClick = { showAllArtists = !showAllArtists },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dynamicAccent),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showAllArtists) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (showAllArtists) "Show Less Artists" else "More Artists (${matchingArtists.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Section 1 (Bottom, closest to search bar): Songs
                        if (matchingSongs.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Songs (${matchingSongs.size})",
                                        color = dynamicAccent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val visibleSongs = if (showAllSongs) matchingSongs else matchingSongs.take(5)
                            items(visibleSongs, key = { it.id }) { song ->
                                SongItemCard(
                                    song = song,
                                    isPlaying = isPlaying,
                                    isCurrent = (currentSong?.id == song.id),
                                    onClick = { viewModel.playSong(song, matchingSongs) },
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

                            if (matchingSongs.size > 5) {
                                item {
                                    OutlinedButton(
                                        onClick = { showAllSongs = !showAllSongs },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dynamicAccent),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showAllSongs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (showAllSongs) "Show Less Songs" else "More Songs (${matchingSongs.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Search Input Bar (Pinned at bottom)
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search songs, artists, albums...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = dynamicAccent) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkCardGlass,
                    unfocusedContainerColor = DarkCardGlass,
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = DarkSurfaceElevated
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_input")
            )
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

private data class SearchAlbumItem(
    val name: String,
    val artist: String,
    val artUri: String?,
    val tracks: List<Song>
)
