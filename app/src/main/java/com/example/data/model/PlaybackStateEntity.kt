package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentSongId: Long? = null,
    val currentPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val queueSongIds: String = "", // Comma-separated or JSON list of IDs
    val currentQueueIndex: Int = 0,
    val repeatMode: Int = 0, // 0 = off, 1 = all, 2 = one
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val eqEnabled: Boolean = false,
    val eqBandLevels: String = "0,0,0,0,0,0,0,0,0,0", // 10 bands
    val eqBassBoost: Short = 0,
    val eqVirtualizer: Short = 0,
    val eqReverbPreset: Short = 0
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val tabBarPosition: String = "BOTTOM", // "TOP" or "BOTTOM"
    val lockScreenEnabled: Boolean = true,
    val touchLockDurationMs: Long = 1000L, // Long press duration (1s default)
    val lockDisableSeekbar: Boolean = true,
    val lockDisablePrevNext: Boolean = true,
    val lockDisablePlayPause: Boolean = false,
    val lyricsFontSize: String = "MEDIUM", // "SMALL", "MEDIUM", "LARGE"
    val widgetTransparent: Boolean = false,
    val widgetOpacity: Float = 0.85f,
    val customScanFolder: String? = null,
    val selectedFolders: String = "",
    val excludedFolders: String = "",
    val tabOrder: String = "PLAYLISTS,SONGS,FOLDERS,ARTISTS,SEARCH,FAVORITES,DUSTY_TRACKS",
    val enabledTabs: String = "PLAYLISTS,SONGS,FOLDERS,ARTISTS,SEARCH,FAVORITES,DUSTY_TRACKS",
    val dialogBlurRadius: Float = 16f
)
