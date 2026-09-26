package com.example.data.tageditor

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

object Id3TagEditor {
    private const val TAG = "Id3TagEditor"

    suspend fun updateSongTags(
        context: Context,
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newGenre: String,
        newLyrics: String,
        newCoverArtBytes: ByteArray?
    ): Song = withContext(Dispatchers.IO) {
        var updatedArtUri = song.albumArtUri
        val file = File(song.path)

        // If new cover art bytes are provided, save to local app storage cache for this song
        if (newCoverArtBytes != null && newCoverArtBytes.isNotEmpty()) {
            try {
                val artDir = File(context.filesDir, "custom_artwork").apply { mkdirs() }
                val artFile = File(artDir, "art_${song.id}.jpg")
                FileOutputStream(artFile).use { it.write(newCoverArtBytes) }
                updatedArtUri = artFile.toURI().toString()
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving cover art image: ${e.message}")
            }
        }

        // Try writing ID3v2 frames to actual file if accessible
        if (file.exists() && file.canWrite()) {
            try {
                writeId3v2Tags(file, newTitle, newArtist, newAlbum, newGenre, newLyrics, newCoverArtBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Direct ID3 file update fallback: ${e.message}")
            }
        }

        song.copy(
            title = newTitle.ifBlank { song.title },
            artist = newArtist.ifBlank { song.artist },
            album = newAlbum.ifBlank { song.album },
            genre = newGenre.ifBlank { song.genre },
            lyrics = newLyrics,
            albumArtUri = updatedArtUri
        )
    }

    private fun writeId3v2Tags(
        file: File,
        title: String,
        artist: String,
        album: String,
        genre: String,
        lyrics: String,
        coverArt: ByteArray?
    ) {
        val frames = mutableListOf<ByteArray>()
        createTextFrame("TIT2", title)?.let { frames.add(it) }
        createTextFrame("TPE1", artist)?.let { frames.add(it) }
        createTextFrame("TALB", album)?.let { frames.add(it) }
        createTextFrame("TCON", genre)?.let { frames.add(it) }
        if (lyrics.isNotBlank()) {
            createLyricsFrame(lyrics)?.let { frames.add(it) }
        }
        if (coverArt != null && coverArt.isNotEmpty()) {
            createPictureFrame(coverArt)?.let { frames.add(it) }
        }

        val totalFramesSize = frames.sumOf { it.size }
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 3 // ID3v2.3
        header[4] = 0 // Revision
        header[5] = 0 // Flags

        // Syncsafe integer for size
        encodeSyncSafe(totalFramesSize, header, 6)

        // Read original audio content past any existing ID3v2 tag
        val (audioOffset, _) = getExistingTagSize(file)
        val audioData = FileInputStream(file).use { fis ->
            fis.skip(audioOffset)
            fis.readBytes()
        }

        // Re-write file with new ID3 tag + existing audio payload
        FileOutputStream(file).use { fos ->
            fos.write(header)
            for (frame in frames) {
                fos.write(frame)
            }
            fos.write(audioData)
        }
    }

    private fun getExistingTagSize(file: File): Pair<Long, Long> {
        if (!file.exists() || file.length() < 10) return Pair(0L, 0L)
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                val size = decodeSyncSafe(header, 6)
                return Pair(10L + size, size.toLong())
            }
        }
        return Pair(0L, 0L)
    }

    private fun createTextFrame(frameId: String, text: String): ByteArray? {
        if (text.isBlank()) return null
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + textBytes.size)
        payload[0] = 3 // UTF-8 encoding flag
        System.arraycopy(textBytes, 0, payload, 1, textBytes.size)

        val frame = ByteArray(10 + payload.size)
        System.arraycopy(frameId.toByteArray(StandardCharsets.US_ASCII), 0, frame, 0, 4)
        encodeInt32(payload.size, frame, 4)
        frame[8] = 0 // Flag
        frame[9] = 0 // Flag
        System.arraycopy(payload, 0, frame, 10, payload.size)
        return frame
    }

    private fun createLyricsFrame(lyrics: String): ByteArray? {
        val lyricsBytes = lyrics.toByteArray(StandardCharsets.UTF_8)
        val lang = "eng".toByteArray(StandardCharsets.US_ASCII)
        val desc = ByteArray(1) // empty description + null terminator
        val payload = ByteArray(1 + lang.size + desc.size + lyricsBytes.size)
        var offset = 0
        payload[offset++] = 3 // UTF-8
        System.arraycopy(lang, 0, payload, offset, lang.size)
        offset += lang.size
        payload[offset++] = 0 // empty description null terminator
        System.arraycopy(lyricsBytes, 0, payload, offset, lyricsBytes.size)

        val frame = ByteArray(10 + payload.size)
        System.arraycopy("USLT".toByteArray(StandardCharsets.US_ASCII), 0, frame, 0, 4)
        encodeInt32(payload.size, frame, 4)
        frame[8] = 0
        frame[9] = 0
        System.arraycopy(payload, 0, frame, 10, payload.size)
        return frame
    }

    private fun createPictureFrame(imageData: ByteArray): ByteArray? {
        val mimeType = "image/jpeg".toByteArray(StandardCharsets.US_ASCII)
        val payloadSize = 1 + mimeType.size + 1 + 1 + 1 + imageData.size
        val payload = ByteArray(payloadSize)
        var offset = 0
        payload[offset++] = 0 // ISO-8859-1 for mime/desc
        System.arraycopy(mimeType, 0, payload, offset, mimeType.size)
        offset += mimeType.size
        payload[offset++] = 0 // Null terminator for mime
        payload[offset++] = 3 // Cover (front) picture type
        payload[offset++] = 0 // Description null terminator
        System.arraycopy(imageData, 0, payload, offset, imageData.size)

        val frame = ByteArray(10 + payload.size)
        System.arraycopy("APIC".toByteArray(StandardCharsets.US_ASCII), 0, frame, 0, 4)
        encodeInt32(payload.size, frame, 4)
        frame[8] = 0
        frame[9] = 0
        System.arraycopy(payload, 0, frame, 10, payload.size)
        return frame
    }

    private fun encodeSyncSafe(value: Int, target: ByteArray, offset: Int) {
        var v = value
        target[offset + 3] = (v and 0x7F).toByte()
        v = v shr 7
        target[offset + 2] = (v and 0x7F).toByte()
        v = v shr 7
        target[offset + 1] = (v and 0x7F).toByte()
        v = v shr 7
        target[offset] = (v and 0x7F).toByte()
    }

    private fun decodeSyncSafe(src: ByteArray, offset: Int): Int {
        var size = 0
        for (i in 0..3) {
            size = (size shl 7) or (src[offset + i].toInt() and 0x7F)
        }
        return size
    }

    private fun encodeInt32(value: Int, target: ByteArray, offset: Int) {
        target[offset] = ((value shr 24) and 0xFF).toByte()
        target[offset + 1] = ((value shr 16) and 0xFF).toByte()
        target[offset + 2] = ((value shr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }
}
