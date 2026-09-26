package com.example.data.scanner

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object LyricsExtractor {
    private const val TAG = "LyricsExtractor"

    fun extractLyrics(filePath: String): String {
        if (filePath.isBlank()) return ""
        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() < 128) return ""

        try {
            // 1. Check adjacent .lrc file first (highest fidelity synchronized lyrics)
            val lrcLyrics = findAdjacentLrc(file)
            if (lrcLyrics.isNotBlank()) return lrcLyrics

            val ext = file.extension.lowercase()
            when (ext) {
                "mp3" -> {
                    val id3Lyrics = extractId3Lyrics(file)
                    if (id3Lyrics.isNotBlank()) return id3Lyrics
                }
                "flac", "ogg", "opus" -> {
                    val vorbisLyrics = extractVorbisLyrics(file)
                    if (vorbisLyrics.isNotBlank()) return vorbisLyrics
                }
                "m4a", "mp4", "aac" -> {
                    val m4aLyrics = extractMp4Lyrics(file)
                    if (m4aLyrics.isNotBlank()) return m4aLyrics
                }
            }

            // Fallback: Check adjacent .txt file with matching name
            val txtLyrics = findAdjacentTxt(file)
            if (txtLyrics.isNotBlank()) return txtLyrics

        } catch (e: Exception) {
            Log.d(TAG, "Failed extracting lyrics from $filePath: ${e.message}")
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

    /**
     * Parses ID3v2 USLT (Unsynchronized lyrics) and SYLT frames from MP3 files.
     */
    private fun extractId3Lyrics(file: File): String {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                return ""
            }

            val versionMajor = header[3].toInt() // 2, 3, or 4
            val tagSize = decodeSyncSafe(header, 6)
            if (tagSize <= 0 || tagSize > 15 * 1024 * 1024) return ""

            val tagBytes = ByteArray(tagSize)
            raf.readFully(tagBytes)

            var offset = 0
            while (offset + 10 < tagBytes.size) {
                val frameId: String
                val frameSize: Int

                if (versionMajor == 2) {
                    frameId = String(tagBytes, offset, 3, StandardCharsets.US_ASCII)
                    frameSize = (tagBytes[offset + 3].toInt() and 0xFF shl 16) or
                            (tagBytes[offset + 4].toInt() and 0xFF shl 8) or
                            (tagBytes[offset + 5].toInt() and 0xFF)
                    offset += 6
                } else {
                    frameId = String(tagBytes, offset, 4, StandardCharsets.US_ASCII)
                    frameSize = if (versionMajor == 4) {
                        decodeSyncSafe(tagBytes, offset + 4)
                    } else {
                        decodeInt32(tagBytes, offset + 4)
                    }
                    offset += 10
                }

                if (frameSize <= 0 || offset + frameSize > tagBytes.size) break

                if (frameId == "USLT" || frameId == "ULT" || frameId == "SYLT") {
                    val framePayload = tagBytes.copyOfRange(offset, offset + frameSize)
                    val lyrics = parseUsltPayload(framePayload)
                    if (lyrics.isNotBlank()) return lyrics
                }

                offset += frameSize
            }
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

        // Skip 1 byte encoding + 3 bytes language
        var idx = 4
        // Find end of content description (null-terminated according to encoding)
        if (encodingByte == 1 || encodingByte == 2) {
            // 2-byte null terminator (0x00, 0x00)
            while (idx + 1 < payload.size) {
                if (payload[idx] == 0.toByte() && payload[idx + 1] == 0.toByte()) {
                    idx += 2
                    break
                }
                idx += 2
            }
        } else {
            // 1-byte null terminator (0x00)
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

    /**
     * Extracts Vorbis comments (e.g. LYRICS= or UNSYNCEDLYRICS=) in FLAC/OGG files.
     */
    private fun extractVorbisLyrics(file: File): String {
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(65536.coerceAtMost(file.length().toInt()))
            val read = fis.read(buffer)
            if (read <= 0) return ""
            val content = String(buffer, 0, read, StandardCharsets.ISO_8859_1)
            val markers = listOf("LYRICS=", "unsyncedlyrics=", "Lyrics=", "UNSYNCEDLYRICS=")
            for (marker in markers) {
                val pos = content.indexOf(marker, ignoreCase = true)
                if (pos >= 0) {
                    val start = pos + marker.length
                    val end = content.indexOf("\u0000", start).takeIf { it > 0 } ?: (start + 2000).coerceAtMost(read)
                    val raw = buffer.copyOfRange(start, end)
                    val lyrics = String(raw, StandardCharsets.UTF_8).trim()
                    if (lyrics.length > 5) return lyrics
                }
            }
        }
        return ""
    }

    /**
     * Extracts ©lyr atom in MP4/M4A containers.
     */
    private fun extractMp4Lyrics(file: File): String {
        RandomAccessFile(file, "r").use { raf ->
            val scanLength = 256 * 1024L.coerceAtMost(file.length())
            val buffer = ByteArray(scanLength.toInt())
            raf.readFully(buffer)
            val pattern = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
            for (i in 0 until buffer.size - pattern.size - 16) {
                if (buffer[i] == pattern[0] && buffer[i + 1] == pattern[1] &&
                    buffer[i + 2] == pattern[2] && buffer[i + 3] == pattern[3]
                ) {
                    // Atom structure: [4 byte size][©lyr][4 byte size][data][flags][reserved][payload]
                    var dataOffset = i + 4
                    while (dataOffset + 8 < buffer.size) {
                        if (buffer[dataOffset + 4] == 'd'.code.toByte() &&
                            buffer[dataOffset + 5] == 'a'.code.toByte() &&
                            buffer[dataOffset + 6] == 't'.code.toByte() &&
                            buffer[dataOffset + 7] == 'a'.code.toByte()
                        ) {
                            val dataSize = decodeInt32(buffer, dataOffset)
                            val textStart = dataOffset + 16
                            val textLength = (dataSize - 16).coerceAtMost(buffer.size - textStart)
                            if (textLength > 0) {
                                return String(buffer, textStart, textLength, StandardCharsets.UTF_8).trim()
                            }
                        }
                        dataOffset++
                    }
                }
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
