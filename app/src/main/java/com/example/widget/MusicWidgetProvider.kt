package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.playback.AudioPlaybackService

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val service = AudioPlaybackService.instance
        WidgetUpdateHelper.updateWidgets(
            context,
            service?.currentSong,
            service?.getNextSong(),
            service?.playerEngine?.isPlaying?.value ?: false,
            service?.playerEngine?.currentPosition?.value ?: 0L,
            service?.playerEngine?.duration?.value ?: 0L
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val service = AudioPlaybackService.instance

        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                if (service != null) {
                    service.playerEngine.togglePlayPause()
                    WidgetUpdateHelper.updateWidgets(
                        context,
                        service.currentSong,
                        service.getNextSong(),
                        service.playerEngine.isPlaying.value,
                        service.playerEngine.currentPosition.value,
                        service.playerEngine.duration.value
                    )
                } else {
                    val serviceIntent = Intent(context, AudioPlaybackService::class.java).apply {
                        action = AudioPlaybackService.ACTION_PLAY_PAUSE
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }

            ACTION_WIDGET_PREV -> {
                if (service != null) {
                    service.playPrevious()
                    WidgetUpdateHelper.updateWidgets(
                        context,
                        service.currentSong,
                        service.getNextSong(),
                        service.playerEngine.isPlaying.value,
                        service.playerEngine.currentPosition.value,
                        service.playerEngine.duration.value
                    )
                } else {
                    val serviceIntent = Intent(context, AudioPlaybackService::class.java).apply {
                        action = AudioPlaybackService.ACTION_PREV
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }

            ACTION_WIDGET_NEXT -> {
                if (service != null) {
                    service.playNext()
                    WidgetUpdateHelper.updateWidgets(
                        context,
                        service.currentSong,
                        service.getNextSong(),
                        service.playerEngine.isPlaying.value,
                        service.playerEngine.currentPosition.value,
                        service.playerEngine.duration.value
                    )
                } else {
                    val serviceIntent = Intent(context, AudioPlaybackService::class.java).apply {
                        action = AudioPlaybackService.ACTION_NEXT
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }

            ACTION_WIDGET_SEEK -> {
                val ratio = intent.getFloatExtra(EXTRA_SEEK_RATIO, 0f)
                if (service != null) {
                    val duration = service.playerEngine.duration.value
                    val targetMs = (duration * ratio).toLong().coerceIn(0L, duration)
                    service.playerEngine.seekTo(targetMs)
                    WidgetUpdateHelper.updateWidgets(
                        context,
                        service.currentSong,
                        service.getNextSong(),
                        service.playerEngine.isPlaying.value,
                        targetMs,
                        duration
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_WIDGET_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_WIDGET_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_WIDGET_SEEK = "com.example.widget.ACTION_SEEK"
        const val EXTRA_SEEK_RATIO = "com.example.widget.EXTRA_SEEK_RATIO"
    }
}
