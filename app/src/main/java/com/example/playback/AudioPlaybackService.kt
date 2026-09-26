package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream

class AudioPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_CLOSE = "com.example.ACTION_CLOSE"
        const val ACTION_SEEK = "com.example.ACTION_SEEK"
        const val ACTION_SEEK_BY = "com.example.ACTION_SEEK_BY"
        const val ACTION_SEEK_PERCENT = "com.example.ACTION_SEEK_PERCENT"
        const val EXTRA_SEEK_POS = "extra_seek_pos"
        const val EXTRA_SEEK_OFFSET = "extra_seek_offset"
        const val EXTRA_SEEK_PERCENT = "extra_seek_percent"

        @Volatile
        var instance: AudioPlaybackService? = null
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    lateinit var repository: MusicRepository
    lateinit var playerEngine: AudioPlayerEngine
    private var mediaSession: MediaSession? = null

    // Callbacks for UI sync
    var onTrackChanged: ((Song?) -> Unit)? = null
    var onQueueChanged: ((List<Song>) -> Unit)? = null

    var currentSong: Song? = null
        private set
    var queue: MutableList<Song> = mutableListOf()
        private set
    var currentQueueIndex: Int = 0
        private set
    var repeatMode: Int = 0 // 0 = off, 1 = all, 2 = one
        private set
    var shuffleEnabled: Boolean = false
        private set
    private var originalQueue: List<Song> = emptyList()

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = MusicRepository(applicationContext)

        playerEngine = AudioPlayerEngine(
            context = applicationContext,
            scope = serviceScope,
            on50PercentPlayed = { songId ->
                serviceScope.launch(Dispatchers.IO) {
                    repository.incrementPlayCount(songId)
                }
            }
        ).apply {
            onTrackEnded = {
                playNext()
            }
        }

        mediaSession = MediaSession.Builder(this, playerEngine.exoPlayer).build()
        mediaSession?.let { addSession(it) }

        createNotificationChannel()
        updateNotification()

        // Sync player changes to notification and widget
        serviceScope.launch {
            playerEngine.isPlaying.collect { isPlaying ->
                updateNotification()
                if (isPlaying) {
                    startWidgetProgressUpdates()
                } else {
                    stopWidgetProgressUpdates()
                }
                WidgetUpdateHelper.updateWidgets(
                    applicationContext,
                    currentSong,
                    getNextSong(),
                    isPlaying,
                    playerEngine.currentPosition.value,
                    playerEngine.duration.value
                )
            }
        }

        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                if (currentSong != null && playerEngine.isPlaying.value) {
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val settings = repository.getAppSettingsSync()
                            if (settings.lockScreenEnabled) {
                                val lockIntent = Intent(applicationContext, com.example.ui.lockscreen.LockScreenActivity::class.java).apply {
                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    )
                                }
                                applicationContext.startActivity(lockIntent)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AudioPlaybackService", "Error launching lock screen: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        updateNotification()
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_PREV -> playPrevious()
            ACTION_NEXT -> playNext()
            ACTION_CLOSE -> {
                // MANDATE: The Close button must kill the foreground service but save current track/position state to Room.
                saveCurrentPlaybackState()
                playerEngine.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SEEK -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POS, 0L)
                playerEngine.seekTo(pos)
            }
            ACTION_SEEK_BY -> {
                val offset = intent.getLongExtra(EXTRA_SEEK_OFFSET, 0L)
                val current = playerEngine.currentPosition.value
                val dur = playerEngine.duration.value
                val target = (current + offset).coerceIn(0L, dur.coerceAtLeast(0L))
                playerEngine.seekTo(target)
            }
            ACTION_SEEK_PERCENT -> {
                val percent = intent.getFloatExtra(EXTRA_SEEK_PERCENT, 0f)
                val dur = playerEngine.duration.value
                val target = (dur * percent).toLong().coerceIn(0L, dur.coerceAtLeast(0L))
                playerEngine.seekTo(target)
            }
        }
        return START_STICKY
    }

    private var widgetProgressJob: Job? = null

    private fun startWidgetProgressUpdates() {
        widgetProgressJob?.cancel()
        widgetProgressJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (playerEngine.isPlaying.value && currentSong != null) {
                    WidgetUpdateHelper.updateWidgets(
                        applicationContext,
                        currentSong,
                        getNextSong(),
                        true,
                        playerEngine.currentPosition.value,
                        playerEngine.duration.value
                    )
                }
            }
        }
    }

    private fun stopWidgetProgressUpdates() {
        widgetProgressJob?.cancel()
        widgetProgressJob = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val isPlaying = playerEngine.isPlaying.value
        val hasTrack = currentSong != null
        if (isPlaying || hasTrack) {
            // Keep playing and keep foreground notification active!
            // Crucially: DO NOT call super.onTaskRemoved(rootIntent) as it triggers stopSelf() in MediaSessionService
            updateNotification()
        } else {
            saveCurrentPlaybackState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, playImmediately: Boolean = true) {
        if (songs.isEmpty()) return
        originalQueue = songs
        queue = if (shuffleEnabled) {
            val selected = songs.getOrNull(startIndex)
            val rest = songs.filter { it.id != selected?.id }.shuffled()
            (listOfNotNull(selected) + rest).toMutableList()
        } else {
            songs.toMutableList()
        }
        currentQueueIndex = if (shuffleEnabled) 0 else startIndex.coerceIn(0, queue.size - 1)
        val song = queue.getOrNull(currentQueueIndex) ?: return
        loadTrack(song, 0L, playImmediately)
        onQueueChanged?.invoke(queue)
    }

    fun playTrack(song: Song) {
        val existingIndex = queue.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            currentQueueIndex = existingIndex
            loadTrack(song, 0L, true)
        } else {
            queue.add(song)
            currentQueueIndex = queue.size - 1
            loadTrack(song, 0L, true)
            onQueueChanged?.invoke(queue)
        }
    }

    fun playQueueIndex(index: Int) {
        if (index in queue.indices) {
            currentQueueIndex = index
            loadTrack(queue[index], 0L, true)
            onQueueChanged?.invoke(queue)
        }
    }

    fun playNextInQueue(song: Song) {
        // "Play Next" inserts the track exactly after current track
        val nextIdx = (currentQueueIndex + 1).coerceAtMost(queue.size)
        queue.add(nextIdx, song)
        onQueueChanged?.invoke(queue)
    }

    fun addToQueueEnd(song: Song) {
        // "Add to Queue" appends to very end
        queue.add(song)
        onQueueChanged?.invoke(queue)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex in queue.indices && toIndex in queue.indices) {
            val moved = queue.removeAt(fromIndex)
            queue.add(toIndex, moved)
            if (currentQueueIndex == fromIndex) {
                currentQueueIndex = toIndex
            } else if (fromIndex < currentQueueIndex && toIndex >= currentQueueIndex) {
                currentQueueIndex--
            } else if (fromIndex > currentQueueIndex && toIndex <= currentQueueIndex) {
                currentQueueIndex++
            }
            onQueueChanged?.invoke(queue)
        }
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            val wasPlaying = (index == currentQueueIndex)
            queue.removeAt(index)
            if (queue.isEmpty()) {
                currentSong = null
                playerEngine.pause()
            } else {
                if (index < currentQueueIndex) {
                    currentQueueIndex--
                } else if (wasPlaying) {
                    currentQueueIndex = currentQueueIndex.coerceIn(0, queue.size - 1)
                    queue.getOrNull(currentQueueIndex)?.let { loadTrack(it, 0L, true) }
                }
            }
            onQueueChanged?.invoke(queue)
            updateNotification()
        }
    }

    fun togglePlayPause() {
        if (currentSong == null && queue.isNotEmpty()) {
            loadTrack(queue[0], 0L, true)
        } else {
            playerEngine.togglePlayPause()
        }
        updateNotification()
    }

    fun playNext() {
        if (queue.isEmpty()) return
        if (repeatMode == 2) { // Repeat One
            playerEngine.seekTo(0L)
            playerEngine.play()
            return
        }
        if (currentQueueIndex + 1 < queue.size) {
            currentQueueIndex++
            loadTrack(queue[currentQueueIndex], 0L, true)
        } else if (repeatMode == 1) { // Repeat All
            currentQueueIndex = 0
            loadTrack(queue[currentQueueIndex], 0L, true)
        } else {
            playerEngine.pause()
            playerEngine.seekTo(0L)
        }
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        if (playerEngine.currentPosition.value > 3000L) {
            playerEngine.seekTo(0L)
            return
        }
        if (currentQueueIndex - 1 >= 0) {
            currentQueueIndex--
            loadTrack(queue[currentQueueIndex], 0L, true)
        } else if (repeatMode == 1) {
            currentQueueIndex = queue.size - 1
            loadTrack(queue[currentQueueIndex], 0L, true)
        } else {
            playerEngine.seekTo(0L)
        }
    }

    fun toggleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        val current = currentSong
        if (shuffleEnabled) {
            val rest = queue.filter { it.id != current?.id }.shuffled()
            queue = (listOfNotNull(current) + rest).toMutableList()
            currentQueueIndex = 0
        } else {
            if (originalQueue.isNotEmpty()) {
                queue = originalQueue.toMutableList()
                currentQueueIndex = queue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
            }
        }
        onQueueChanged?.invoke(queue)
    }

    fun loadTrack(song: Song, startPosition: Long = 0L, playImmediately: Boolean = false) {
        currentSong = song
        if (queue.getOrNull(currentQueueIndex)?.id != song.id) {
            val matchingIdx = queue.indexOfFirst { it.id == song.id }
            if (matchingIdx != -1) {
                currentQueueIndex = matchingIdx
            }
        }
        playerEngine.prepareTrack(song, startPosition, playImmediately)
        onTrackChanged?.invoke(song)
        onQueueChanged?.invoke(queue)
        updateNotification()
        WidgetUpdateHelper.updateWidgets(
            applicationContext,
            currentSong,
            getNextSong(),
            playerEngine.isPlaying.value,
            playerEngine.currentPosition.value,
            playerEngine.duration.value
        )
    }

    fun getNextSong(): Song? {
        if (queue.isEmpty()) return null
        return if (currentQueueIndex + 1 < queue.size) {
            queue[currentQueueIndex + 1]
        } else if (repeatMode == 1) {
            queue.firstOrNull()
        } else null
    }

    fun saveCurrentPlaybackState() {
        val song = currentSong ?: return
        serviceScope.launch(Dispatchers.IO) {
            val queueIdsStr = queue.joinToString(",") { it.id.toString() }
            val state = com.example.data.model.PlaybackStateEntity(
                currentSongId = song.id,
                currentPosition = playerEngine.currentPosition.value,
                isPlaying = false,
                queueSongIds = queueIdsStr,
                currentQueueIndex = currentQueueIndex,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                playbackSpeed = playerEngine.playbackSpeed.value,
                eqEnabled = playerEngine.eqEnabled.value,
                eqBandLevels = playerEngine.bandLevels.value.joinToString(","),
                eqBassBoost = playerEngine.bassBoostLevel.value,
                eqVirtualizer = playerEngine.virtualizerLevel.value,
                eqReverbPreset = playerEngine.reverbPreset.value
            )
            repository.savePlaybackState(state)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media player notification with seek and playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val song = currentSong
        val isPlaying = playerEngine.isPlaying.value
        val title = song?.title ?: "Local Music Player"
        val artist = song?.artist ?: "No track playing"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val closeIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_CLOSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)

        // Add Album Art bitmap if available
        getCoverArtBitmap(song?.albumArtUri)?.let {
            builder.setLargeIcon(it)
        }

        // Seekbar / Progress in notification
        val duration = (song?.duration ?: 0L).toInt()
        val currentPos = playerEngine.currentPosition.value.toInt()
        if (duration > 0) {
            builder.setProgress(duration, currentPos, false)
        }

        // Actions: Prev, Play/Pause, Next, Close (X)
        // MUST NOT include Cast or Favorite buttons per PRD!
        builder.addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
        builder.addAction(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "Pause" else "Play",
            playPauseIntent
        )
        builder.addAction(R.drawable.ic_skip_next, "Next", nextIntent)
        builder.addAction(R.drawable.ic_close, "Close", closeIntent)

        // Media style with 0, 1, 2 action indices in compact view
        mediaSession?.let { session ->
            val mediaStyle = androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun getCoverArtBitmap(uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        saveCurrentPlaybackState()
        mediaSession?.let { session ->
            removeSession(session)
            session.release()
        }
        mediaSession = null
        playerEngine.release()
        serviceJob.cancel()
        instance = null
        super.onDestroy()
    }
}
