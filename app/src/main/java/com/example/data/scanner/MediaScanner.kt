package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ScanResult(
    val addedCount: Int,
    val deletedCount: Int,
    val totalCount: Int
)

object MediaScanner {
    private const val TAG = "MediaScanner"

    suspend fun scanLocalMedia(
        context: Context,
        songDao: SongDao,
        selectedFolders: Set<String> = emptySet(),
        excludedFolders: Set<String> = emptySet(),
        onProgress: (folderPath: String) -> Unit = {}
    ): ScanResult = withContext(Dispatchers.IO) {
        val scannedSongs = mutableListOf<Song>()
        val existingSongs = songDao.getAllSongsList()
        val existingIds = existingSongs.map { it.id }.toSet()
        var addedCount = 0
        var deletedCount = 0

        // Strict Requirement: Default library starts with 0 folders.
        // If user has not selected any folders, library remains completely empty.
        if (selectedFolders.isEmpty()) {
            if (existingSongs.isNotEmpty()) {
                val toDelete = existingSongs.map { it.id }
                songDao.deleteSongsByIds(toDelete)
                deletedCount = toDelete.size
            }
            return@withContext ScanResult(
                addedCount = 0,
                deletedCount = deletedCount,
                totalCount = 0
            )
        }

        var lastReportedFolder = ""

        // 1. Scan MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Title"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""
                    if (path.isBlank()) continue

                    // Only index tracks that reside within user selected folders
                    val isInSelected = selectedFolders.any { selected ->
                        path.startsWith(selected, ignoreCase = true)
                    }
                    if (!isInSelected) continue

                    // Skip any excluded folders
                    if (excludedFolders.isNotEmpty() && excludedFolders.any { excluded ->
                        path.startsWith(excluded, ignoreCase = true)
                    }) {
                        continue
                    }

                    val dateAdded = c.getLong(dateAddedCol)
                    val dateModified = c.getLong(dateModCol)
                    val albumId = c.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val folderPath = File(path).parent ?: ""

                    if (folderPath.isNotBlank() && folderPath != lastReportedFolder) {
                        lastReportedFolder = folderPath
                        onProgress(folderPath)
                    }

                    scannedSongs.add(
                        Song(
                            id = id,
                            uri = contentUri,
                            path = path,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            albumId = albumId,
                            albumArtUri = albumArtUri,
                            folderPath = folderPath,
                            lyrics = extractEmbeddedLyrics(context, path, contentUri)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}")
        }

        // 2. Direct folder scan for files within selectedFolders (in case MediaStore has not indexed them yet)
        selectedFolders.forEach { folderPath ->
            try {
                val dir = File(folderPath)
                if (dir.exists() && dir.isDirectory) {
                    val audioExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus")
                    dir.walkTopDown().maxDepth(10).forEach { file ->
                        if (file.isFile && file.extension.lowercase() in audioExtensions) {
                            val fPath = file.absolutePath
                            val isExcluded = excludedFolders.isNotEmpty() && excludedFolders.any { fPath.startsWith(it, ignoreCase = true) }
                            if (!isExcluded && scannedSongs.none { it.path.equals(fPath, ignoreCase = true) }) {
                                val genId = kotlin.math.abs(fPath.hashCode().toLong()) + 1000000000L
                                val parentDir = file.parent ?: ""
                                if (parentDir.isNotBlank() && parentDir != lastReportedFolder) {
                                    lastReportedFolder = parentDir
                                    onProgress(parentDir)
                                }
                                scannedSongs.add(
                                    Song(
                                        id = genId,
                                        uri = Uri.fromFile(file).toString(),
                                        path = fPath,
                                        title = file.nameWithoutExtension,
                                        artist = "Unknown Artist",
                                        album = file.parentFile?.name ?: "Unknown Album",
                                        duration = 0L,
                                        dateAdded = file.lastModified() / 1000L,
                                        dateModified = file.lastModified() / 1000L,
                                        albumId = 0L,
                                        albumArtUri = null,
                                        folderPath = parentDir,
                                        lyrics = extractEmbeddedLyrics(context, fPath, Uri.fromFile(file).toString())
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct directory scan error: ${e.message}")
            }
        }

        // 3. Remove deleted songs from database (files that no longer exist or are now excluded)
        val scannedIds = scannedSongs.map { it.id }.toSet()
        for (existing in existingSongs) {
            val file = File(existing.path)
            val isInSelected = selectedFolders.any { existing.path.startsWith(it, ignoreCase = true) }
            val isExcluded = excludedFolders.isNotEmpty() && excludedFolders.any { existing.path.startsWith(it, ignoreCase = true) }
            if (existing.id !in scannedIds || !isInSelected || isExcluded || !file.exists()) {
                songDao.deleteSongById(existing.id)
                deletedCount++
            }
        }

        // 3.5. Unify album art across songs belonging to the same album
        val albumArtMap = mutableMapOf<String, String>()
        for (song in scannedSongs) {
            val key = "${song.album.trim().lowercase()}_${song.artist.trim().lowercase()}"
            if (!song.albumArtUri.isNullOrBlank() && !albumArtMap.containsKey(key)) {
                albumArtMap[key] = song.albumArtUri
            }
        }
        for (existing in existingSongs) {
            val key = "${existing.album.trim().lowercase()}_${existing.artist.trim().lowercase()}"
            if (!existing.albumArtUri.isNullOrBlank() && !albumArtMap.containsKey(key)) {
                albumArtMap[key] = existing.albumArtUri
            }
        }

        // 4. Upsert to Room DB while preserving play counts, favorites, and custom lyrics
        for (song in scannedSongs) {
            val key = "${song.album.trim().lowercase()}_${song.artist.trim().lowercase()}"
            val sharedArt = albumArtMap[key]
            val resolvedSong = if (song.albumArtUri.isNullOrBlank() && !sharedArt.isNullOrBlank()) {
                song.copy(albumArtUri = sharedArt)
            } else song

            if (resolvedSong.id !in existingIds) {
                addedCount++
            }
            val existing = songDao.getSongById(resolvedSong.id)
            if (existing != null) {
                val merged = resolvedSong.copy(
                    playCount = existing.playCount,
                    lastPlayedTimestamp = existing.lastPlayedTimestamp,
                    isFavorite = existing.isFavorite,
                    lyrics = if (existing.lyrics.isNotBlank()) existing.lyrics else resolvedSong.lyrics,
                    albumArtUri = existing.albumArtUri ?: resolvedSong.albumArtUri ?: sharedArt
                )
                songDao.updateSong(merged)
            } else {
                songDao.insertSongs(listOf(resolvedSong))
            }
        }

        ScanResult(
            addedCount = addedCount,
            deletedCount = deletedCount,
            totalCount = scannedSongs.size
        )
    }

    private fun extractEmbeddedLyrics(context: Context, filePath: String, uriString: String? = null): String {
        return LyricsExtractor.extractLyrics(context, filePath, uriString)
    }

    private fun ensureDemoTracks(context: Context): List<Song> {
        val demoDir = File(context.filesDir, "demo_music").apply { mkdirs() }
        val demoSongs = mutableListOf<Song>()

        val trackSpecs = listOf(
            Triple("Midnight Nebula", "Aura Collective", 440.0),
            Triple("Cyber Pulse", "Synthetica", 330.0),
            Triple("Velvet Echoes", "Lunar Drifter", 523.25),
            Triple("Dusty Horizons", "Vinyl Nostalgia", 392.0)
        )

        for ((index, spec) in trackSpecs.withIndex()) {
            val (title, artist, freq) = spec
            val file = File(demoDir, "demo_track_${index + 1}.wav")
            if (!file.exists() || file.length() < 1000) {
                generateSineWavFile(file, freq, durationSeconds = 35)
            }

            val lyrics = when (index) {
                0 -> """
                    [00:00.00] In the midnight nebula
                    [00:06.00] Floating through the neon sky
                    [00:12.00] Stars align across the dark
                    [00:18.00] Lost inside the audio arc
                    [00:24.00] Soundwaves guide our quiet flight
                    [00:30.00] Eternal drift into the night
                """.trimIndent()
                1 -> """
                    [00:00.00] Circuits humming in the dark
                    [00:05.00] Electrical electric spark
                    [00:10.00] Pulse beats fast, neon glow
                    [00:15.00] Down the highway we will go
                    [00:22.00] Synthesizer takes control
                    [00:28.00] Pure vibration for the soul
                """.trimIndent()
                else -> """
                    Dust settling on the vinyl grooves
                    A forgotten melody that still moves
                    Timeless rhythm from long ago
                    Spinning softly in the warm glow
                """.trimIndent()
            }

            val song = Song(
                id = 900000L + index,
                uri = file.toURI().toString(),
                path = file.absolutePath,
                title = title,
                artist = artist,
                album = "Pulse Showcase",
                duration = 35000L,
                dateAdded = System.currentTimeMillis() - (index * 86400000L),
                dateModified = System.currentTimeMillis() - (index * 86400000L),
                albumId = 100L,
                folderPath = demoDir.absolutePath,
                genre = "Synthwave",
                lyrics = lyrics,
                playCount = if (index == 3) 0 else index, // Track 4 is dusty!
                lastPlayedTimestamp = if (index == 3) 0L else System.currentTimeMillis() - (index * 100000L),
                isFavorite = (index == 0)
            )
            demoSongs.add(song)
        }

        return demoSongs
    }

    private fun generateSineWavFile(targetFile: File, baseFreq: Double, durationSeconds: Int) {
        try {
            val sampleRate = 44100
            val numSamples = sampleRate * durationSeconds
            val pcmData = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                // Dual tone harmonic chord for warm pleasant synthesizer sound
                val angle1 = 2.0 * Math.PI * baseFreq * time
                val angle2 = 2.0 * Math.PI * (baseFreq * 1.25) * time
                val angle3 = 2.0 * Math.PI * (baseFreq * 1.5) * time
                val sample = ((Math.sin(angle1) * 0.4 + Math.sin(angle2) * 0.3 + Math.sin(angle3) * 0.3) * Short.MAX_VALUE).toInt()
                pcmData[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            FileOutputStream(targetFile).use { fos ->
                writeWavHeader(fos, sampleRate, 1, 16, numSamples * 2)
                val byteBuffer = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmData) {
                    byteBuffer.putShort(s)
                }
                fos.write(byteBuffer.array())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating demo audio: ${e.message}")
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        pcmDataSize: Int
    ) {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        // "RIFF"
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // "WAVE"
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // format size = 16
        header[20] = 1; header[21] = 0 // PCM format = 1
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = ((channels * bitsPerSample) / 8).toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        // "data"
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmDataSize and 0xff).toByte()
        header[41] = ((pcmDataSize shr 8) and 0xff).toByte()
        header[42] = ((pcmDataSize shr 16) and 0xff).toByte()
        header[43] = ((pcmDataSize shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
