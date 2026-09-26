package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.AppSettingsEntity
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.data.scanner.ScanResult
import com.example.playback.AudioPlaybackService
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGray
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class TabType(val label: String) {
    PLAYLISTS("Playlists"),
    SONGS("Songs"),
    FOLDERS("Folders"),
    ARTISTS("Artists"),
    SEARCH("Search"),
    FAVORITES("Favorites"),
    DUSTY_TRACKS("Dusty Tracks")
}

enum class SortBy {
    TITLE,
    DATE_MODIFIED,
    DATE_ADDED,
    DURATION,
    PLAY_COUNT
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class ViewLayoutMode {
    LIST_NORMAL,
    LIST_COMPACT,
    GRID_3,
    GRID_4
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private var service: AudioPlaybackService? = null

    // Service binding
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AudioPlaybackService.LocalBinder
            service = localBinder?.getService()
            observeService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    // Navigation & UI States
    // Default State: App opens directly to 'Now Playing' with the last played track loaded (paused)
    val isNowPlayingExpanded = MutableStateFlow(true)
    val selectedTab = MutableStateFlow(TabType.SONGS)
    val isQueueOpen = MutableStateFlow(false)
    val isEqualizerOpen = MutableStateFlow(false)
    val songForTagEditor = MutableStateFlow<Song?>(null)
    val isSettingsOpen = MutableStateFlow(false)
    val isScanning = MutableStateFlow(false)
    val scanSummaryState = MutableStateFlow<ScanResult?>(null)

    // SharedPreferences for persistent UI tab settings
    private val tabPrefs = getApplication<Application>().getSharedPreferences("prism_music_tab_settings", Context.MODE_PRIVATE)

    private fun loadTabSortBy(key: String, default: SortBy): SortBy {
        val name = tabPrefs.getString("sort_$key", default.name) ?: default.name
        return try { SortBy.valueOf(name) } catch (_: Exception) { default }
    }

    private fun loadTabSortDirection(key: String, default: SortDirection): SortDirection {
        val name = tabPrefs.getString("dir_$key", default.name) ?: default.name
        return try { SortDirection.valueOf(name) } catch (_: Exception) { default }
    }

    private fun saveTabSort(key: String, sort: SortBy, dir: SortDirection) {
        tabPrefs.edit()
            .putString("sort_$key", sort.name)
            .putString("dir_$key", dir.name)
            .apply()
    }

    private fun loadTabLayoutMode(key: String, default: ViewLayoutMode): ViewLayoutMode {
        val name = tabPrefs.getString("layout_$key", default.name) ?: default.name
        return try { ViewLayoutMode.valueOf(name) } catch (_: Exception) { default }
    }

    fun setLayoutMode(key: String, mode: ViewLayoutMode) {
        tabPrefs.edit().putString("layout_$key", mode.name).apply()
        when (key) {
            "songs" -> songsLayoutMode.value = mode
            "favorites" -> favoritesLayoutMode.value = mode
            "dusty" -> dustyLayoutMode.value = mode
            "folders" -> foldersLayoutMode.value = mode
            "artists" -> artistsLayoutMode.value = mode
            "playlists" -> playlistsLayoutMode.value = mode
        }
    }

    // Independent Layout Modes per Tab (loaded from persistence)
    val songsLayoutMode = MutableStateFlow(loadTabLayoutMode("songs", ViewLayoutMode.LIST_NORMAL))
    val favoritesLayoutMode = MutableStateFlow(loadTabLayoutMode("favorites", ViewLayoutMode.LIST_NORMAL))
    val dustyLayoutMode = MutableStateFlow(loadTabLayoutMode("dusty", ViewLayoutMode.LIST_NORMAL))
    val foldersLayoutMode = MutableStateFlow(loadTabLayoutMode("folders", ViewLayoutMode.LIST_NORMAL))
    val artistsLayoutMode = MutableStateFlow(loadTabLayoutMode("artists", ViewLayoutMode.LIST_NORMAL))
    val playlistsLayoutMode = MutableStateFlow(loadTabLayoutMode("playlists", ViewLayoutMode.GRID_3))
    val songLayoutMode = songsLayoutMode // backwards compatibility

    // Independent Sorting per Tab (loaded from persistence)
    val songsSortBy = MutableStateFlow(loadTabSortBy("songs", SortBy.TITLE))
    val songsSortDirection = MutableStateFlow(loadTabSortDirection("songs", SortDirection.ASCENDING))

    val foldersSortBy = MutableStateFlow(loadTabSortBy("folders", SortBy.TITLE))
    val foldersSortDirection = MutableStateFlow(loadTabSortDirection("folders", SortDirection.ASCENDING))

    val artistsSortBy = MutableStateFlow(loadTabSortBy("artists", SortBy.TITLE))
    val artistsSortDirection = MutableStateFlow(loadTabSortDirection("artists", SortDirection.ASCENDING))

    val playlistsSortBy = MutableStateFlow(loadTabSortBy("playlists", SortBy.TITLE))
    val playlistsSortDirection = MutableStateFlow(loadTabSortDirection("playlists", SortDirection.ASCENDING))

    val favoritesSortBy = MutableStateFlow(loadTabSortBy("favorites", SortBy.TITLE))
    val favoritesSortDirection = MutableStateFlow(loadTabSortDirection("favorites", SortDirection.ASCENDING))

    val dustySortBy = MutableStateFlow(loadTabSortBy("dusty", SortBy.PLAY_COUNT))
    val dustySortDirection = MutableStateFlow(loadTabSortDirection("dusty", SortDirection.ASCENDING))

    // Fallback/Legacy Sort flows (synced with songs sort)
    val sortBy = songsSortBy
    val sortDirection = songsSortDirection

    fun setTabSort(tabKey: String, newSortBy: SortBy? = null, newSortDir: SortDirection? = null) {
        when (tabKey) {
            "songs" -> {
                if (newSortBy != null) songsSortBy.value = newSortBy
                if (newSortDir != null) songsSortDirection.value = newSortDir
                saveTabSort("songs", songsSortBy.value, songsSortDirection.value)
            }
            "folders" -> {
                if (newSortBy != null) foldersSortBy.value = newSortBy
                if (newSortDir != null) foldersSortDirection.value = newSortDir
                saveTabSort("folders", foldersSortBy.value, foldersSortDirection.value)
            }
            "artists" -> {
                if (newSortBy != null) artistsSortBy.value = newSortBy
                if (newSortDir != null) artistsSortDirection.value = newSortDir
                saveTabSort("artists", artistsSortBy.value, artistsSortDirection.value)
            }
            "playlists" -> {
                if (newSortBy != null) playlistsSortBy.value = newSortBy
                if (newSortDir != null) playlistsSortDirection.value = newSortDir
                saveTabSort("playlists", playlistsSortBy.value, playlistsSortDirection.value)
            }
            "favorites" -> {
                if (newSortBy != null) favoritesSortBy.value = newSortBy
                if (newSortDir != null) favoritesSortDirection.value = newSortDir
                saveTabSort("favorites", favoritesSortBy.value, favoritesSortDirection.value)
            }
            "dusty" -> {
                if (newSortBy != null) dustySortBy.value = newSortBy
                if (newSortDir != null) dustySortDirection.value = newSortDir
                saveTabSort("dusty", dustySortBy.value, dustySortDirection.value)
            }
        }
    }

    val defaultTabOrder = listOf(
        TabType.PLAYLISTS,
        TabType.SONGS,
        TabType.FOLDERS,
        TabType.ARTISTS,
        TabType.SEARCH,
        TabType.FAVORITES,
        TabType.DUSTY_TRACKS
    )
    val tabOrder = MutableStateFlow<List<TabType>>(defaultTabOrder)
    val enabledTabs = MutableStateFlow<Set<TabType>>(defaultTabOrder.toSet())
    val visibleTabs = MutableStateFlow<List<TabType>>(defaultTabOrder)
    val dialogBlurRadius = MutableStateFlow(16f)

    val selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val excludedFolders = MutableStateFlow<Set<String>>(emptySet())

    val playlistGridColumns = MutableStateFlow(2)
    val m3uImportMessage = MutableStateFlow<String?>(null)

    // Sub-view detail navigation
    val selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedFolder = MutableStateFlow<String?>(null)
    val selectedArtist = MutableStateFlow<String?>(null)

    // Lyrics
    val showLyrics = MutableStateFlow(false)
    val lyricsFontSize = MutableStateFlow("MEDIUM") // "SMALL", "MEDIUM", "LARGE"

    // Search
    val searchQuery = MutableStateFlow("")

    // Player State flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _dynamicAccentColor = MutableStateFlow<Color>(AccentGray)
    val dynamicAccentColor: StateFlow<Color> = _dynamicAccentColor.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Equalizer flows
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _bandLevels = MutableStateFlow(IntArray(10) { 0 })
    val bandLevels: StateFlow<IntArray> = _bandLevels.asStateFlow()

    private val _bassBoost = MutableStateFlow<Short>(0)
    val bassBoost: StateFlow<Short> = _bassBoost.asStateFlow()

    private val _virtualizer = MutableStateFlow<Short>(0)
    val virtualizer: StateFlow<Short> = _virtualizer.asStateFlow()

    private val _reverbPreset = MutableStateFlow<Short>(0)
    val reverbPreset: StateFlow<Short> = _reverbPreset.asStateFlow()

    // Database flows
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dustyTracks: StateFlow<List<Song>> = repository.dustyTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val detectedFolders: StateFlow<List<String>> = allSongs.map { songs ->
        songs.mapNotNull { it.folderPath.takeIf { p -> p.isNotBlank() } }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootFolders: StateFlow<List<String>> = allSongs.map { songs ->
        songs.mapNotNull { it.folderPath.takeIf { p -> p.isNotBlank() } }
            .map { path ->
                val parts = path.split(java.io.File.separator).filter { it.isNotBlank() }
                if (parts.size >= 4 && parts[0] == "storage" && parts[1] == "emulated") {
                    java.io.File.separator + parts.take(4).joinToString(java.io.File.separator)
                } else if (parts.size >= 2) {
                    java.io.File.separator + parts.take(2).joinToString(java.io.File.separator)
                } else path
            }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateVisibleTabs() {
        val enabled = enabledTabs.value
        val visible = tabOrder.value.filter { it in enabled }
        visibleTabs.value = if (visible.isNotEmpty()) visible else listOf(TabType.SONGS)
    }

    fun toggleTabEnabled(tab: TabType) {
        val current = enabledTabs.value.toMutableSet()
        if (current.contains(tab)) {
            if (current.size > 1) {
                current.remove(tab)
            }
        } else {
            current.add(tab)
        }
        enabledTabs.value = current
        updateVisibleTabs()
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getAppSettingsSync()
            repository.saveSettings(s.copy(enabledTabs = current.joinToString(",") { it.name }))
        }
    }

    fun setDialogBlurRadius(radius: Float) {
        dialogBlurRadius.value = radius
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getAppSettingsSync()
            repository.saveSettings(s.copy(dialogBlurRadius = radius))
        }
    }

    fun autoScanAndImportAllPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = android.os.Environment.getExternalStorageDirectory()
                val m3uFiles = mutableListOf<java.io.File>()
                if (root.exists() && root.canRead()) {
                    root.walkTopDown().maxDepth(8).forEach { file ->
                        if (file.isFile && (file.extension.equals("m3u", ignoreCase = true) || file.extension.equals("m3u8", ignoreCase = true))) {
                            m3uFiles.add(file)
                        }
                    }
                }
                var count = 0
                for (f in m3uFiles) {
                    try {
                        importM3uPlaylist(android.net.Uri.fromFile(f))
                        count++
                    } catch (_: Exception) {}
                }
                m3uImportMessage.value = "Auto-scanned device: Imported $count playlist(s)"
            } catch (e: Exception) {
                m3uImportMessage.value = "Playlist auto-scan failed: ${e.message}"
            }
        }
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val current = tabOrder.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            tabOrder.value = current
            updateVisibleTabs()
            viewModelScope.launch(Dispatchers.IO) {
                val s = repository.getAppSettingsSync()
                repository.saveSettings(s.copy(tabOrder = current.joinToString(",") { it.name }))
            }
        }
    }

    fun addSelectedFolder(folderPath: String) {
        val current = selectedFolders.value.toMutableSet()
        current.add(folderPath)
        selectedFolders.value = current
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getAppSettingsSync()
            repository.saveSettings(s.copy(selectedFolders = current.joinToString("\n")))
            refreshMedia()
        }
    }

    fun removeSelectedFolder(folderPath: String) {
        val current = selectedFolders.value.toMutableSet()
        current.remove(folderPath)
        selectedFolders.value = current
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getAppSettingsSync()
            repository.saveSettings(s.copy(selectedFolders = current.joinToString("\n")))
            if (current.isEmpty()) {
                repository.clearAllSongs()
            } else {
                refreshMedia()
            }
        }
    }

    fun toggleExcludeFolder(folderPath: String) {
        val current = excludedFolders.value.toMutableSet()
        if (current.contains(folderPath)) {
            current.remove(folderPath)
        } else {
            current.add(folderPath)
        }
        excludedFolders.value = current
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getAppSettingsSync()
            repository.saveSettings(s.copy(excludedFolders = current.joinToString("\n")))
            refreshMedia()
        }
    }

    // Sorted and filtered songs
    val sortedSongs: StateFlow<List<Song>> = combine(allSongs, sortBy, sortDirection) { songs, sort, dir ->
        val sorted = when (sort) {
            SortBy.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortBy.DATE_MODIFIED -> songs.sortedBy { it.dateModified }
            SortBy.DATE_ADDED -> songs.sortedBy { it.dateAdded }
            SortBy.DURATION -> songs.sortedBy { it.duration }
            SortBy.PLAY_COUNT -> songs.sortedBy { it.playCount }
        }
        if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results flow
    val searchResults: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Start and bind service
        val context = getApplication<Application>()
        WidgetUpdateHelper.initSettings(context)
        val serviceIntent = Intent(context, AudioPlaybackService::class.java)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initial media scan & state restoration
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val s = repository.getAppSettingsSync()
                    if (s.tabOrder.isNotBlank()) {
                        val parsed = s.tabOrder.split(",").mapNotNull { name ->
                            try { TabType.valueOf(name.trim()) } catch (_: Exception) { null }
                        }
                        if (parsed.isNotEmpty()) {
                            val missing = defaultTabOrder.filter { it !in parsed }
                            tabOrder.value = parsed + missing
                        }
                    }
                    if (s.enabledTabs.isNotBlank()) {
                        val parsedEn = s.enabledTabs.split(",").mapNotNull { name ->
                            try { TabType.valueOf(name.trim()) } catch (_: Exception) { null }
                        }.toSet()
                        if (parsedEn.isNotEmpty()) {
                            enabledTabs.value = parsedEn
                        }
                    }
                    dialogBlurRadius.value = s.dialogBlurRadius
                    updateVisibleTabs()

                    val sel = if (s.selectedFolders.isNotBlank()) {
                        s.selectedFolders.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    } else emptySet()
                    selectedFolders.value = sel

                    val excl = if (s.excludedFolders.isNotBlank()) {
                        s.excludedFolders.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    } else emptySet()
                    excludedFolders.value = excl

                    if (sel.isNotEmpty()) {
                        repository.scanMedia(selectedFolders = sel, excludedFolders = excl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                restoreLastPlaybackState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Observe current song and extract dominant color dynamically + auto-load lyrics
        viewModelScope.launch {
            _currentSong.collect { song ->
                extractColorFromAlbumArt(song)
                if (song != null && song.lyrics.isBlank()) {
                    checkAndLoadLyrics(song)
                }
            }
        }
    }

    fun checkAndLoadLyrics(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val extracted = com.example.data.scanner.LyricsExtractor.extractLyrics(getApplication(), song.path, song.uri)
            if (extracted.isNotBlank()) {
                val updated = song.copy(lyrics = extracted)
                repository.updateSong(updated)
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = updated
                }
            }
        }
    }

    fun loadLyricsForCurrentSong() {
        val song = _currentSong.value ?: return
        checkAndLoadLyrics(song)
    }

    fun updateSongLyrics(songId: Long, newLyrics: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = repository.getSongById(songId) ?: return@launch
            val updated = song.copy(lyrics = newLyrics)
            repository.updateSong(updated)
            if (_currentSong.value?.id == songId) {
                _currentSong.value = updated
            }
            com.example.util.AppLogger.i("MusicViewModel", "Updated lyrics for song id=$songId")
        }
    }

    private fun observeService() {
        val s = service ?: return
        s.onTrackChanged = { song ->
            _currentSong.value = song
            _currentQueueIndex.value = s.currentQueueIndex
        }
        s.onQueueChanged = { q ->
            _queue.value = q.toList()
            _currentQueueIndex.value = s.currentQueueIndex
        }

        viewModelScope.launch {
            s.playerEngine.isPlaying.collect { _isPlaying.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.currentPosition.collect { _currentPosition.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.duration.collect { _duration.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.playbackSpeed.collect { _playbackSpeed.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.eqEnabled.collect { _eqEnabled.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.bandLevels.collect { _bandLevels.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.bassBoostLevel.collect { _bassBoost.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.virtualizerLevel.collect { _virtualizer.value = it }
        }
        viewModelScope.launch {
            s.playerEngine.reverbPreset.collect { _reverbPreset.value = it }
        }

        _repeatMode.value = s.repeatMode
        _shuffleEnabled.value = s.shuffleEnabled
        if (s.currentSong != null) {
            _currentSong.value = s.currentSong
        } else if (_currentSong.value != null && s.queue.isEmpty()) {
            s.setQueue(_queue.value, _currentQueueIndex.value, playImmediately = false)
            s.playerEngine.seekTo(_currentPosition.value)
        }

        if (s.queue.isNotEmpty()) {
            _queue.value = s.queue.toList()
        } else if (_queue.value.isNotEmpty()) {
            s.setQueue(_queue.value, _currentQueueIndex.value, playImmediately = false)
        }
        _currentQueueIndex.value = s.currentQueueIndex
    }

    private suspend fun restoreLastPlaybackState() {
        val (savedState, allTracks) = withContext(Dispatchers.IO) {
            val state = repository.getPlaybackStateSync()
            val tracks = repository.allSongs.firstOrNull() ?: emptyList()
            Pair(state, tracks)
        }
        if (allTracks.isEmpty()) return

        val targetSong = withContext(Dispatchers.IO) {
            if (savedState?.currentSongId != null) {
                repository.getSongById(savedState.currentSongId) ?: allTracks.firstOrNull()
            } else {
                allTracks.firstOrNull()
            }
        } ?: return

        // Parse saved queue
        val queueList = withContext(Dispatchers.IO) {
            if (!savedState?.queueSongIds.isNullOrBlank()) {
                val ids = savedState.queueSongIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                val fetchedMap = repository.getSongsByIds(ids).associateBy { it.id }
                val ordered = ids.mapNotNull { fetchedMap[it] }
                if (ordered.isNotEmpty()) ordered else allTracks
            } else {
                allTracks
            }
        }

        val startPos = savedState?.currentPosition ?: 0L
        val targetIdx = if (savedState != null && savedState.currentQueueIndex in queueList.indices) {
            savedState.currentQueueIndex
        } else {
            queueList.indexOfFirst { it.id == targetSong.id }.coerceAtLeast(0)
        }

        // Prepare paused on Now Playing
        service?.setQueue(queueList, targetIdx, playImmediately = false)
        service?.playerEngine?.seekTo(startPos)
        _currentSong.value = queueList.getOrNull(targetIdx) ?: targetSong
        _currentPosition.value = startPos
        _duration.value = (_currentSong.value ?: targetSong).duration
        _queue.value = queueList

        // Restore EQ settings if saved
        if (savedState != null) {
            service?.playerEngine?.setEqualizerEnabled(savedState.eqEnabled)
            val bands = savedState.eqBandLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
            bands.forEachIndexed { index, level ->
                service?.playerEngine?.setBandLevel(index, level)
            }
            service?.playerEngine?.setBassBoost(savedState.eqBassBoost)
            service?.playerEngine?.setVirtualizer(savedState.eqVirtualizer)
            service?.playerEngine?.setReverbPreset(savedState.eqReverbPreset)
            service?.playerEngine?.setSpeed(savedState.playbackSpeed)
        }
    }

    // Playback actions
    fun playSong(song: Song, contextQueue: List<Song>? = null) {
        val q = contextQueue ?: sortedSongs.value
        val idx = q.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        service?.setQueue(q, idx, playImmediately = true)
        _currentSong.value = song
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        service?.togglePlayPause()
    }

    fun playNext() {
        service?.playNext()
    }

    fun playPrevious() {
        service?.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        service?.playerEngine?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setSpeed(speed: Float) {
        service?.playerEngine?.setSpeed(speed)
    }

    fun toggleRepeat() {
        service?.toggleRepeatMode()
        _repeatMode.value = service?.repeatMode ?: 0
    }

    fun toggleShuffle() {
        service?.toggleShuffle()
        _shuffleEnabled.value = service?.shuffleEnabled ?: false
        _queue.value = service?.queue?.toList() ?: emptyList()
    }

    fun playNextInQueue(song: Song) {
        service?.playNextInQueue(song)
    }

    fun addToQueueEnd(song: Song) {
        service?.addToQueueEnd(song)
    }

    fun reorderQueue(from: Int, to: Int) {
        val current = _queue.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val moved = current.removeAt(from)
            current.add(to, moved)
            _queue.value = current
        }
        service?.reorderQueue(from, to)
        viewModelScope.launch(Dispatchers.IO) {
            val qIds = _queue.value.joinToString(",") { it.id.toString() }
            val curId = _currentSong.value?.id ?: -1L
            repository.savePlaybackState(
                PlaybackStateEntity(
                    currentSongId = curId,
                    currentPosition = _currentPosition.value,
                    queueSongIds = qIds,
                    repeatMode = _repeatMode.value,
                    shuffleEnabled = _shuffleEnabled.value,
                    playbackSpeed = _playbackSpeed.value
                )
            )
        }
    }

    fun removeFromQueue(index: Int) {
        service?.removeFromQueue(index)
    }

    fun playQueueIndex(index: Int) {
        service?.playQueueIndex(index)
    }

    // Favorite & Delete
    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFav = !song.isFavorite
            repository.toggleFavorite(song.id, newFav)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = newFav)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentSong.value?.id == song.id) {
                playNext()
            }
            repository.deleteSong(song.id)
        }
    }

    // Scan
    fun refreshMedia(customFolder: String? = null, showNotification: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            isScanning.value = true
            val context = getApplication<Application>()
            val notificationManager = NotificationManagerCompat.from(context)
            val channelId = "media_scan_channel"

            if (showNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Media Scanner",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Media library scanner status"
                    setShowBadge(false)
                }
                val sysManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                sysManager?.createNotificationChannel(channel)
            }

            val scanNotifId = 2002
            val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true

            fun updateNotification(text: String, isOngoing: Boolean) {
                if (!showNotification || !hasNotifPermission) return
                try {
                    val notif = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification_small)
                        .setContentTitle(if (isOngoing) "Scanning Media Library..." else "Media Scan Complete")
                        .setContentText(text)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .setOngoing(isOngoing)
                        .setAutoCancel(!isOngoing)
                        .setOnlyAlertOnce(true)
                        .apply {
                            if (isOngoing) setProgress(0, 0, true)
                        }
                        .build()
                    notificationManager.notify(scanNotifId, notif)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (showNotification) {
                updateNotification("Starting scan...", true)
            }

            var lastReportedTime = 0L
            val scanTargets = if (customFolder != null) selectedFolders.value + customFolder else selectedFolders.value
            val result = repository.scanMedia(scanTargets, excludedFolders.value) { currentFolder ->
                val now = System.currentTimeMillis()
                if (now - lastReportedTime > 300) {
                    lastReportedTime = now
                    updateNotification(currentFolder, true)
                }
            }

            if (showNotification) {
                val finishText = if (result.addedCount > 0 || result.deletedCount > 0) {
                    "${result.addedCount} new songs found, ${result.deletedCount} removed"
                } else {
                    "Library is up to date (${result.totalCount} tracks)"
                }
                updateNotification(finishText, false)
            }

            isScanning.value = false
            scanSummaryState.value = result
        }
    }

    // Playlists
    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(name)
        }
    }

    fun updatePlaylistDetails(playlistId: Long, name: String, customCoverUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlaylistDetails(playlistId, name, customCoverUri)
            if (selectedPlaylist.value?.id == playlistId) {
                selectedPlaylist.value = selectedPlaylist.value?.copy(name = name, customCoverUri = customCoverUri)
            }
        }
    }

    fun importM3uPlaylist(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var derivedName = ""
                // 1. Try file path or last path segment
                val pathSegment = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast('\\')
                if (!pathSegment.isNullOrBlank()) {
                    derivedName = pathSegment.substringBeforeLast(".")
                }

                // 2. Try content resolver query
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                cursor.getString(nameIndex)?.let {
                                    val name = it.substringBeforeLast(".")
                                    if (name.isNotBlank()) derivedName = name
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (derivedName.isBlank()) {
                    derivedName = "Playlist_${System.currentTimeMillis() % 10000}"
                }

                val songsInDb = repository.allSongs.firstOrNull() ?: emptyList()
                val matchedSongIds = mutableListOf<Long>()
                var m3uPlaylistHeaderTitle: String? = null

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.startsWith("#PLAYLIST:", ignoreCase = true)) {
                            val headerTitle = trimmed.substringAfter(":").trim()
                            if (headerTitle.isNotBlank()) {
                                m3uPlaylistHeaderTitle = headerTitle
                            }
                            continue
                        }
                        if (trimmed.isBlank() || trimmed.startsWith("#")) continue

                        val targetFilename = trimmed.substringAfterLast("/").substringAfterLast("\\").lowercase()
                        val targetTitle = targetFilename.substringBeforeLast(".")

                        val match = songsInDb.firstOrNull { s ->
                            s.path.equals(trimmed, ignoreCase = true) ||
                            s.path.substringAfterLast("/").equals(targetFilename, ignoreCase = true) ||
                            s.title.equals(targetTitle, ignoreCase = true)
                        }
                        if (match != null && match.id !in matchedSongIds) {
                            matchedSongIds.add(match.id)
                        }
                    }
                }

                val finalPlaylistName = m3uPlaylistHeaderTitle ?: derivedName

                if (matchedSongIds.isNotEmpty()) {
                    val playlistId = repository.createPlaylist(finalPlaylistName)
                    matchedSongIds.forEachIndexed { idx, id ->
                        repository.addSongToPlaylist(playlistId, id, idx)
                    }
                    m3uImportMessage.value = "Playlist \"$finalPlaylistName\" imported with ${matchedSongIds.size} songs."
                } else {
                    m3uImportMessage.value = "No matching songs from playlist \"$finalPlaylistName\" found in local storage."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                m3uImportMessage.value = "Error reading file: ${e.localizedMessage}"
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlist.id)
            if (selectedPlaylist.value?.id == playlist.id) {
                selectedPlaylist.value = null
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun createPlaylistFromQueue(name: String, onCreated: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistId = repository.createPlaylist(name)
            val currentQ = _queue.value
            currentQ.forEachIndexed { index, song ->
                repository.addSongToPlaylist(playlistId, song.id, index)
            }
            withContext(Dispatchers.Main) {
                onCreated?.invoke()
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeSongFromPlaylist(playlistId, song.id)
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = repository.getSongsForPlaylist(playlistId)

    // Tag Editor
    fun saveTags(
        song: Song,
        title: String,
        artist: String,
        album: String,
        genre: String,
        lyrics: String,
        coverArtBytes: ByteArray?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = repository.updateSongTags(
                song, title, artist, album, genre, lyrics, coverArtBytes
            )
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = updated
            }
            songForTagEditor.value = null
        }
    }

    // Equalizer settings
    fun setEqEnabled(enabled: Boolean) {
        service?.playerEngine?.setEqualizerEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Int, level: Int) {
        service?.playerEngine?.setBandLevel(bandIndex, level)
    }

    fun setBassBoost(level: Short) {
        service?.playerEngine?.setBassBoost(level)
    }

    fun setVirtualizer(level: Short) {
        service?.playerEngine?.setVirtualizer(level)
    }

    fun setReverbPreset(preset: Short) {
        service?.playerEngine?.setReverbPreset(preset)
    }

    // App Settings
    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(settings)
            WidgetUpdateHelper.setOpacityAndRefresh(getApplication(), settings.widgetOpacity)
        }
    }

    private fun extractColorFromAlbumArt(song: Song?) {
        if (song == null) {
            _dynamicAccentColor.value = AccentGray
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var extractedColor: Color? = null
            val context = getApplication<Application>()

            // 1. Android 10+ (API 29+) MediaStore thumbnail from song.uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && song.uri.isNotBlank()) {
                try {
                    val bitmap = context.contentResolver.loadThumbnail(
                        Uri.parse(song.uri),
                        android.util.Size(256, 256),
                        null
                    )
                    extractedColor = findDominantVibrantColor(bitmap)
                } catch (_: Exception) {}
            }

            // 2. MediaMetadataRetriever embedded picture from song.uri
            if (extractedColor == null && song.uri.isNotBlank()) {
                try {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(context, Uri.parse(song.uri))
                    val artBytes = mmr.embeddedPicture
                    mmr.release()
                    if (artBytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                        if (bitmap != null) {
                            extractedColor = findDominantVibrantColor(bitmap)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. openInputStream on albumArtUri if available
            if (extractedColor == null && !song.albumArtUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(song.albumArtUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }
                        val bitmap = BitmapFactory.decodeStream(stream, null, options)
                        if (bitmap != null) {
                            extractedColor = findDominantVibrantColor(bitmap)
                        }
                    }
                } catch (_: Exception) {}
            }

            // 4. Vibrant fallback: If no cover art exists, generate a deterministic beautiful vibrant color based on song/album
            if (extractedColor == null) {
                val seed = (song.album.ifEmpty { song.artist } + song.title).hashCode()
                val hue = kotlin.math.abs(seed % 360).toFloat()
                val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.78f, 0.92f))
                extractedColor = Color(rgb)
            }

            _dynamicAccentColor.value = extractedColor ?: AccentGray
        }
    }

    private fun findDominantVibrantColor(bitmap: Bitmap): Color? {
        val width = bitmap.width
        val height = bitmap.height
        val stepX = maxOf(1, width / 24)
        val stepY = maxOf(1, height / 24)
        val hsv = FloatArray(3)

        var bestColor: Color? = null
        var maxScore = -1f

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                // Filter out non-colors: very dark, pure white, or washed out gray
                if (value < 0.22f || (value > 0.95f && sat < 0.15f) || sat < 0.20f) {
                    continue
                }

                // Balance saturation and pleasing brightness
                val score = sat * 2.0f + (1.0f - kotlin.math.abs(value - 0.75f))
                if (score > maxScore) {
                    maxScore = score
                    val finalValue = value.coerceIn(0.68f, 0.95f)
                    val finalSat = sat.coerceIn(0.55f, 1.0f)
                    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, finalSat, finalValue))
                    bestColor = Color(rgb)
                }
            }
        }
        return bestColor
    }

    // Multi-Selection State for Songs, Folders, and Artists
    val selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    fun toggleSongSelection(songId: Long) {
        val current = selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
            if (current.isEmpty()) {
                isSelectionMode.value = false
            }
        } else {
            current.add(songId)
            isSelectionMode.value = true
        }
        selectedSongIds.value = current
    }

    fun selectAllSongs(songs: List<Song>) {
        val allIds = songs.map { it.id }.toSet()
        if (selectedSongIds.value.size == allIds.size) {
            selectedSongIds.value = emptySet()
            isSelectionMode.value = false
        } else {
            selectedSongIds.value = allIds
            isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        selectedSongIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun playSelectedSongsNow(songs: List<Song>) {
        val selected = songs.filter { it.id in selectedSongIds.value }
        if (selected.isNotEmpty()) {
            playSong(selected.first(), selected)
        }
        clearSelection()
    }

    fun playSelectedSongsNext(songs: List<Song>) {
        val selected = songs.filter { it.id in selectedSongIds.value }
        selected.reversed().forEach { song ->
            service?.playNextInQueue(song)
        }
        clearSelection()
    }

    fun addSelectedSongsToQueue(songs: List<Song>) {
        val selected = songs.filter { it.id in selectedSongIds.value }
        selected.forEach { song ->
            service?.addToQueueEnd(song)
        }
        clearSelection()
    }

    fun addSelectedSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        val selected = songs.filter { it.id in selectedSongIds.value }
        viewModelScope.launch(Dispatchers.IO) {
            selected.forEachIndexed { index, song ->
                repository.addSongToPlaylist(playlistId, song.id, index)
            }
            com.example.util.AppLogger.i("MusicViewModel", "Added ${selected.size} selected songs to playlist $playlistId")
        }
        clearSelection()
    }

    fun deleteSelectedSongs(context: Context? = null, songs: List<Song>) {
        val selected = songs.filter { it.id in selectedSongIds.value }
        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach { song ->
                try {
                    deleteSong(song)
                } catch (e: Exception) {
                    com.example.util.AppLogger.w("MusicViewModel", "Failed to delete song ${song.title}", e)
                }
            }
            com.example.util.AppLogger.i("MusicViewModel", "Deleted ${selected.size} selected songs")
        }
        clearSelection()
    }

    override fun onCleared() {
        service?.saveCurrentPlaybackState()
        val context = getApplication<Application>()
        try {
            context.unbindService(serviceConnection)
        } catch (_: Exception) {}
        super.onCleared()
    }
}
