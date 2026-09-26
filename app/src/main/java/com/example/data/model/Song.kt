package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val uri: String,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val albumId: Long = 0L,
    val albumArtUri: String? = null,
    val folderPath: String = "",
    val genre: String = "Unknown",
    val lyrics: String = "",
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (duration / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
