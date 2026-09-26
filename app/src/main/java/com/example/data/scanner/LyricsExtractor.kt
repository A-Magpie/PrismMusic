package com.example.data.scanner

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object LyricsExtractor {
    private const val TAG = "LyricsExtractor"

    fun extractLyrics(context: Context?, filePath: String?, uriString: String?): String {
        // 1. First priority: Check adjacent .lrc / .txt files if a local file path exists
        if (!filePath.isNullOrBlank()) {
            try {
                val file = File(filePath)
                val lrc = findAdjacentLrc(file)
                if (lrc.isNotBlank()) return lrc

                val txt = findAdjacentTxt(file)
                if (txt.isNotBlank()) return txt
            } catch (e: Exception) {
                Log.d(TAG, "Adjacent file check failed: ${e.message}")
            }
        }

        // 2. Second priority: Direct binary stream parsing (ID3v2 USLT/SYLT, Vorbis, MP4 atom)
        val streamLyrics = extractFromStream(context, filePath, uriString)
        if (streamLyrics.isNotBlank()) {
            return streamLyrics
        }

        return ""
    }

    // Backwards-compatible overload
    fun extractLyrics(filePath: String): String {
        return extractLyrics(null, filePath, null)
    }

    /**
     * Opens an InputStream (via ContentResolver or direct file) and inspects ID3v2 / Vorbis / MP4 tags.
     */
    private fun extractFromStream(context: Context?, filePath: String?, uriString: String?): String {
        fun openStream(): InputStream? {
            if (context != null && !uriString.isNullOrBlank()) {
                try {
                    val stream = context.contentResolver.openInputStream(Uri.parse(uriString))
                    if (stream != null) return stream
                } catch (_: Exception) {}
            }
            if (!filePath.isNullOrBlank()) {
                val file = File(filePath)
                if (file.exists() && file.canRead()) {
                    try {
                        return file.inputStream()
                    } catch (_: Exception) {}
                }
            }
            return null
        }

        try {
            // Check ID3v2 tag (MP3)
            openStream()?.use { input ->
                val id3Lyrics = parseId3Stream(input)
                if (id3Lyrics.isNotBlank()) return id3Lyrics
            }

            // Check Vorbis comments (FLAC / OGG / OPUS)
            openStream()?.use { input ->
                val vorbisLyrics = parseVorbisStream(input)
                if (vorbisLyrics.isNotBlank()) return vorbisLyrics
            }

            // Check MP4 / M4A ©lyr atom
            openStream()?.use { input ->
                val mp4Lyrics = parseMp4Stream(input)
                if (mp4Lyrics.isNotBlank()) return mp4Lyrics
            }
        } catch (e: Exception) {
            Log.d(TAG, "Stream extraction error: ${e.message}")
        }
        return ""
    }

    private fun parseId3Stream(input: InputStream): String {
        val header = ByteArray(10)
        var readTotal = 0
        while (readTotal < 10) {
            val r = input.read(header, readTotal, 10 - readTotal)
            if (r <= 0) return ""
            readTotal += r
        }

        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return ""
        }

        val versionMajor = header[3].toInt() // 2, 3, or 4
        val tagSize = decodeSyncSafe(header, 6)
        if (tagSize <= 0 || tagSize > 12 * 1024 * 1024) return ""

        val tagBytes = ByteArray(tagSize)
        var offset = 0
        while (offset < tagSize) {
            val r = input.read(tagBytes, offset, tagSize - offset)
            if (r <= 0) break
            offset += r
        }

        var pos = 0
        while (pos + 10 < tagBytes.size) {
            val frameId: String
            val frameSize: Int

            if (versionMajor == 2) {
                frameId = String(tagBytes, pos, 3, StandardCharsets.US_ASCII)
                frameSize = (tagBytes[pos + 3].toInt() and 0xFF shl 16) or
                        (tagBytes[pos + 4].toInt() and 0xFF shl 8) or
                        (tagBytes[pos + 5].toInt() and 0xFF)
                pos += 6
            } else {
                frameId = String(tagBytes, pos, 4, StandardCharsets.US_ASCII)
                frameSize = if (versionMajor == 4) {
                    decodeSyncSafe(tagBytes, pos + 4)
                } else {
                    decodeInt32(tagBytes, pos + 4)
                }
                pos += 10
            }

            if (frameSize <= 0 || pos + frameSize > tagBytes.size) break

            if (frameId == "USLT" || frameId == "ULT" || frameId == "SYLT") {
                val framePayload = tagBytes.copyOfRange(pos, pos + frameSize)
                val lyrics = parseUsltPayload(framePayload)
                if (lyrics.isNotBlank()) return lyrics
            }

            pos += frameSize
        }
        return ""
    }

    private fun parseUsltPayload(payload: ByteArray): String {
        if (payload.size < 5) return ""
        val encodingByte = payload[0].toInt()
        val charset: Charset = when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }

        var idx = 4 // skip 1 byte encoding + 3 bytes language
        if (encodingByte == 1 || encodingByte == 2) {
            // Skip 2-byte null terminator for description
            while (idx + 1 < payload.size) {
                if (payload[idx] == 0.toByte() && payload[idx + 1] == 0.toByte()) {
                    idx += 2
                    break
                }
                idx += 2
            }
        } else {
            // Skip 1-byte null terminator
            while (idx < payload.size) {
                if (payload[idx] == 0.toByte()) {
                    idx += 1
                    break
                }
                idx++
            }
        }

        if (idx >= payload.size) return ""
        val lyricsBytes = payload.copyOfRange(idx, payload.size)
        return String(lyricsBytes, charset).trim()
    }

    private fun parseVorbisStream(input: InputStream): String {
        val buffer = ByteArray(65536)
        val read = input.read(buffer)
        if (read <= 0) return ""
        val content = String(buffer, 0, read, StandardCharsets.ISO_8859_1)
        val markers = listOf("LYRICS=", "unsyncedlyrics=", "Lyrics=", "UNSYNCEDLYRICS=")
        for (marker in markers) {
            val pos = content.indexOf(marker, ignoreCase = true)
            if (pos >= 0) {
                val start = pos + marker.length
                val end = content.indexOf("\u0000", start).takeIf { it > 0 } ?: (start + 2500).coerceAtMost(read)
                val raw = buffer.copyOfRange(start, end)
                val lyrics = String(raw, StandardCharsets.UTF_8).trim()
                if (lyrics.length > 5) return lyrics
            }
        }
        return ""
    }

    private fun parseMp4Stream(input: InputStream): String {
        val buffer = ByteArray(256 * 1024)
        val read = input.read(buffer)
        if (read <= 16) return ""
        val pattern = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
        for (i in 0 until read - pattern.size - 16) {
            if (buffer[i] == pattern[0] && buffer[i + 1] == pattern[1] &&
                buffer[i + 2] == pattern[2] && buffer[i + 3] == pattern[3]
            ) {
                var dataOffset = i + 4
                while (dataOffset + 8 < read) {
                    if (buffer[dataOffset + 4] == 'd'.code.toByte() &&
                        buffer[dataOffset + 5] == 'a'.code.toByte() &&
                        buffer[dataOffset + 6] == 't'.code.toByte() &&
                        buffer[dataOffset + 7] == 'a'.code.toByte()
                    ) {
                        val dataSize = decodeInt32(buffer, dataOffset)
                        val textStart = dataOffset + 16
                        val textLength = (dataSize - 16).coerceAtMost(read - textStart)
                        if (textLength > 0) {
                            return String(buffer, textStart, textLength, StandardCharsets.UTF_8).trim()
                        }
                    }
                    dataOffset++
                }
            }
        }
        return ""
    }

    private fun findAdjacentLrc(file: File): String {
        val parent = file.parentFile ?: return ""
        val baseName = file.nameWithoutExtension
        val candidates = listOf(
            File(parent, "$baseName.lrc"),
            File(parent, "$baseName.LRC"),
            File(parent, "$baseName.Lrc")
        )
        for (c in candidates) {
            if (c.exists() && c.canRead()) {
                val text = c.readText(StandardCharsets.UTF_8).trim()
                if (text.isNotBlank()) return text
            }
        }
        return ""
    }

    private fun findAdjacentTxt(file: File): String {
        val parent = file.parentFile ?: return ""
        val baseName = file.nameWithoutExtension
        val txtFile = File(parent, "$baseName.txt")
        if (txtFile.exists() && txtFile.canRead() && txtFile.length() in 20..100000) {
            val text = txtFile.readText(StandardCharsets.UTF_8).trim()
            if (text.isNotBlank() && (text.contains("\n") || text.contains("["))) {
                return text
            }
        }
        return ""
    }

    private fun decodeSyncSafe(src: ByteArray, offset: Int): Int {
        var size = 0
        for (i in 0..3) {
            size = (size shl 7) or (src[offset + i].toInt() and 0x7F)
        }
        return size
    }

    private fun decodeInt32(src: ByteArray, offset: Int): Int {
        return (src[offset].toInt() and 0xFF shl 24) or
                (src[offset + 1].toInt() and 0xFF shl 16) or
                (src[offset + 2].toInt() and 0xFF shl 8) or
                (src[offset + 3].toInt() and 0xFF)
    }
}
