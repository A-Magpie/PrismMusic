package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.InputStream

object WidgetUpdateHelper {

    @Volatile
    private var cachedOpacity: Float = 0.88f

    fun initSettings(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val settings = db.appSettingsDao().getSettings().firstOrNull()
                if (settings != null) {
                    cachedOpacity = settings.widgetOpacity.coerceIn(0.0f, 1.0f)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateWidgets(
        context: Context,
        currentSong: Song?,
        nextSong: Song?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ) {
        if (currentSong == null) {
            CoroutineScope(Dispatchers.IO).launch {
                var loadedSong: Song? = null
                var loadedPos = 0L
                try {
                    val db = AppDatabase.getInstance(context)
                    val state = db.playbackStateDao().getPlaybackStateSync()
                    if (state?.currentSongId != null) {
                        loadedSong = db.songDao().getSongById(state.currentSongId)
                        loadedPos = state.currentPosition
                    }
                } catch (_: Exception) {}

                renderWidgets(
                    context = context,
                    currentSong = loadedSong,
                    nextSong = nextSong,
                    isPlaying = false,
                    position = loadedPos,
                    duration = loadedSong?.duration ?: 0L
                )
            }
        } else {
            renderWidgets(context, currentSong, nextSong, isPlaying, position, duration)
        }
    }

    private fun renderWidgets(
        context: Context,
        currentSong: Song?,
        nextSong: Song?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val componentName = ComponentName(context, MusicWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

        if (appWidgetIds.isEmpty()) return

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.music_widget_layout)

            // Dynamic background opacity slider support (0.0f = completely transparent)
            val alphaInt = (cachedOpacity * 255).toInt().coerceIn(0, 255)
            if (alphaInt == 0) {
                views.setViewVisibility(R.id.widget_bg_layer, android.view.View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_bg_layer, android.view.View.VISIBLE)
                views.setInt(R.id.widget_bg_layer, "setImageAlpha", alphaInt)
            }

            // Open App on click for info container
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_cover, openAppPendingIntent)

            // Track title & artist
            if (currentSong != null) {
                views.setTextViewText(R.id.widget_title, currentSong.title)
                views.setTextViewText(R.id.widget_artist, currentSong.artist)

                // Seekbar progress (0 to 1000)
                val dur = if (duration > 0) duration else currentSong.duration
                val progressPercent = if (dur > 0) ((position.toFloat() / dur) * 1000).toInt() else 0
                views.setProgressBar(R.id.widget_progress, 1000, progressPercent.coerceIn(0, 1000), false)

                // Rounded square album art (1:1 square with 12dp rounded corners)
                val rawArt = loadBitmap(context, currentSong.albumArtUri)
                if (rawArt != null) {
                    val roundedArt = roundSquareBitmap(rawArt, 12f * context.resources.displayMetrics.density)
                    views.setImageViewBitmap(R.id.widget_cover, roundedArt)
                } else {
                    views.setImageViewResource(R.id.widget_cover, R.drawable.ic_album_placeholder)
                }
            } else {
                views.setTextViewText(R.id.widget_title, "No Track Playing")
                views.setTextViewText(R.id.widget_artist, "Tap to open player")
                views.setProgressBar(R.id.widget_progress, 1000, 0, false)
                views.setImageViewResource(R.id.widget_cover, R.drawable.ic_album_placeholder)
            }

            // Controls via Broadcast to MusicWidgetProvider (Prev, Play/Pause, Next)
            val prevIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = MusicWidgetProvider.ACTION_WIDGET_PREV
            }
            val prevPending = PendingIntent.getBroadcast(
                context, 201, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = MusicWidgetProvider.ACTION_WIDGET_PLAY_PAUSE
            }
            val playPausePending = PendingIntent.getBroadcast(
                context, 202, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending)
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = MusicWidgetProvider.ACTION_WIDGET_NEXT
            }
            val nextPending = PendingIntent.getBroadcast(
                context, 203, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

            // Touchable Seekbar: 10 segments with specific seek ratios (0.05, 0.15, ..., 0.95)
            val seekSegmentIds = intArrayOf(
                R.id.widget_seek_0, R.id.widget_seek_1, R.id.widget_seek_2, R.id.widget_seek_3, R.id.widget_seek_4,
                R.id.widget_seek_5, R.id.widget_seek_6, R.id.widget_seek_7, R.id.widget_seek_8, R.id.widget_seek_9
            )
            for (i in seekSegmentIds.indices) {
                val ratio = (i + 0.5f) / 10.0f
                val seekIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                    action = MusicWidgetProvider.ACTION_WIDGET_SEEK
                    putExtra(MusicWidgetProvider.EXTRA_SEEK_RATIO, ratio)
                }
                val seekPending = PendingIntent.getBroadcast(
                    context, 300 + i, seekIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(seekSegmentIds[i], seekPending)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    fun setOpacityAndRefresh(context: Context, opacity: Float) {
        cachedOpacity = opacity.coerceIn(0.1f, 1.0f)
        val service = com.example.playback.AudioPlaybackService.instance
        updateWidgets(
            context,
            service?.currentSong,
            service?.getNextSong(),
            service?.playerEngine?.isPlaying?.value ?: false,
            service?.playerEngine?.currentPosition?.value ?: 0L,
            service?.playerEngine?.duration?.value ?: 0L
        )
    }

    private fun loadBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            stream?.use {
                val original = BitmapFactory.decodeStream(it) ?: return null
                val targetSize = 200
                val w = original.width
                val h = original.height
                if (w > targetSize || h > targetSize) {
                    val scale = targetSize.toFloat() / maxOf(w, h)
                    val scaledW = (w * scale).toInt().coerceAtLeast(1)
                    val scaledH = (h * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(original, scaledW, scaledH, true)
                } else {
                    original
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun roundSquareBitmap(src: Bitmap, cornerRadius: Float): Bitmap {
        return try {
            val size = minOf(src.width, src.height)
            val startX = (src.width - size) / 2
            val startY = (src.height - size) / 2
            val square = Bitmap.createBitmap(src, startX, startY, size, size)

            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
            val path = Path()
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawBitmap(square, 0f, 0f, paint)
            output
        } catch (_: Exception) {
            src
        }
    }
}
