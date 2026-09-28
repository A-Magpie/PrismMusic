package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import com.example.data.scanner.MediaScanner
import com.example.data.scanner.ScanResult
import com.example.data.tageditor.Id3TagEditor
import com.example.util.AppLogger
import kotlinx.coroutines.flow.Flow
import java.io.File

class MusicRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val playbackStateDao = db.playbackStateDao()
    private val appSettingsDao = db.appSettingsDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val dustyTracks: Flow<List<Song>> = songDao.getDustyTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val playbackState: Flow<PlaybackStateEntity?> = playbackStateDao.getPlaybackState()
    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    suspend fun getPlaybackStateSync(): PlaybackStateEntity? = playbackStateDao.getPlaybackStateSync()

    suspend fun getAppSettingsSync(): AppSettingsEntity {
        return appSettingsDao.getSettingsSync() ?: AppSettingsEntity().also {
            appSettingsDao.saveSettings(it)
        }
    }

    suspend fun scanMedia(
        selectedFolders: Set<String> = emptySet(),
        excludedFolders: Set<String> = emptySet(),
        onProgress: (folderPath: String) -> Unit = {}
    ): ScanResult {
        return MediaScanner.scanLocalMedia(context, songDao, selectedFolders, excludedFolders, onProgress)
    }

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    suspend fun getSongsByIds(ids: List<Long>): List<Song> = songDao.getSongsByIds(ids)

    suspend fun toggleFavorite(songId: Long, isFav: Boolean) {
        songDao.setFavorite(songId, isFav)
    }

    suspend fun incrementPlayCount(songId: Long) {
        songDao.incrementPlayCount(songId, System.currentTimeMillis())
    }

    suspend fun deleteSong(songId: Long): Boolean {
        val song = songDao.getSongById(songId)
        var fileDeleted = false
        if (song != null) {
            try {
                val file = File(song.path)
                if (file.exists()) {
                    fileDeleted = file.delete()
                    AppLogger.i("MusicRepository", "Direct File.delete for '${song.title}': $fileDeleted")
                }
            } catch (e: Exception) {
                AppLogger.w("MusicRepository", "Direct file.delete failed for ${song.path}", e)
            }

            try {
                val uri = if (song.uri.isNotBlank()) {
                    Uri.parse(song.uri)
                } else {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                }
                val count = context.contentResolver.delete(uri, null, null)
                if (count > 0) fileDeleted = true
                AppLogger.i("MusicRepository", "contentResolver.delete rows affected: $count for uri: $uri")
            } catch (e: Exception) {
                AppLogger.w("MusicRepository", "contentResolver.delete failed for ${song.uri}", e)
            }
        }
        songDao.deleteSongById(songId)
        return fileDeleted
    }

    suspend fun updateSongTags(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newLyrics: String,
        coverArtBytes: ByteArray?
    ): Song {
        val updated = Id3TagEditor.updateSongTags(
            context,
            song,
            newTitle,
            newArtist,
            newAlbum,
            newGenre,
            newLyrics,
            coverArtBytes
        )
        songDao.updateSong(updated)
        // If album cover was updated, share with siblings in the same album
        if (updated.album.isNotBlank() && !updated.albumArtUri.isNullOrBlank()) {
            val siblings = songDao.getSongsByAlbum(updated.album)
            siblings.filter { it.id != updated.id }.forEach { sibling ->
                songDao.updateSong(sibling.copy(albumArtUri = updated.albumArtUri))
            }
        }
        return updated
    }

    suspend fun updateSong(song: Song) {
        songDao.updateSong(song)
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, order: Int = 0) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, order))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun updatePlaylistDetails(playlistId: Long, name: String, customCoverUri: String?) {
        playlistDao.updatePlaylistDetails(playlistId, name, customCoverUri)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun savePlaybackState(state: PlaybackStateEntity) {
        playbackStateDao.savePlaybackState(state)
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        appSettingsDao.saveSettings(settings)
    }

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    suspend fun clearAllSongs() {
        songDao.clearAllSongs()
    }
}
