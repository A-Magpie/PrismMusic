package com.example.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val on50PercentPlayed: (Long) -> Unit
) {
    companion object {
        private const val TAG = "AudioPlayerEngine"
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        // Lightweight, no crossfade, standard playback
        repeatMode = Player.REPEAT_MODE_OFF
        shuffleModeEnabled = false
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // State flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // 50% play count tracker flags
    private var currentSongId: Long? = null
    private var hasCountedForCurrentTrack = false

    var onTrackEnded: (() -> Unit)? = null

    // Equalizer & FX
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    // 10 bands normalized levels (-100 to 100)
    private val _bandLevels = MutableStateFlow(IntArray(10) { 0 })
    val bandLevels: StateFlow<IntArray> = _bandLevels.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow<Short>(0)
    val bassBoostLevel: StateFlow<Short> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow<Short>(0)
    val virtualizerLevel: StateFlow<Short> = _virtualizerLevel.asStateFlow()

    private val _reverbPreset = MutableStateFlow<Short>(0)
    val reverbPreset: StateFlow<Short> = _reverbPreset.asStateFlow()

    private var progressJob: Job? = null

    // Receiver for headset disconnect (becoming noisy)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d(TAG, "Audio becoming noisy (headset disconnected) -> PAUSE (no auto-resume)")
                pause()
            }
        }
    }

    // Audio focus change listener
    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost ($focusChange) -> PAUSE")
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // MANDATE: MUST NOT Auto-resume when focus returns! Require manual play.
                Log.d(TAG, "Audio focus regained -> NOT auto-resuming per specification")
            }
        }
    }

    init {
        // Register becoming noisy receiver
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            ContextCompat.registerReceiver(
                context,
                noisyReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register noisy receiver: ${e.message}")
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    setupAudioFx(exoPlayer.audioSessionId)
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Track ended -> invoking onTrackEnded")
                    runOnMainThread {
                        onTrackEnded?.invoke()
                    }
                }
            }
        })
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    fun prepareTrack(song: Song, startPosition: Long = 0L, playWhenReady: Boolean = false) {
        runOnMainThread {
            currentSongId = song.id
            hasCountedForCurrentTrack = false

            val mediaItem = MediaItem.fromUri(Uri.parse(song.uri))
            // Always pass resetPosition = true so ExoPlayer resets from STATE_ENDED
            exoPlayer.setMediaItem(mediaItem, true)
            exoPlayer.prepare()
            val targetPos = startPosition.coerceAtLeast(0L)
            exoPlayer.seekTo(targetPos)
            _currentPosition.value = targetPos
            _duration.value = song.duration

            if (playWhenReady) {
                play()
            } else {
                exoPlayer.playWhenReady = false
            }
        }
    }

    fun play() {
        runOnMainThread {
            if (requestAudioFocus()) {
                exoPlayer.play()
            }
        }
    }

    fun pause() {
        runOnMainThread {
            exoPlayer.pause()
            abandonAudioFocus()
        }
    }

    fun togglePlayPause() {
        runOnMainThread {
            if (exoPlayer.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        runOnMainThread {
            exoPlayer.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }

    fun setSpeed(speed: Float) {
        runOnMainThread {
            _playbackSpeed.value = speed
            exoPlayer.playbackParameters = PlaybackParameters(speed)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val pos = exoPlayer.currentPosition
                _currentPosition.value = pos
                val dur = exoPlayer.duration.coerceAtLeast(1L)

                // 50% Play counter listener requirement:
                // If a track plays past the 50% mark, increment play count by +1
                if (!hasCountedForCurrentTrack && pos >= (dur / 2)) {
                    hasCountedForCurrentTrack = true
                    currentSongId?.let { songId ->
                        Log.d(TAG, "50% duration reached for track $songId -> increment play count")
                        on50PercentPlayed(songId)
                    }
                }

                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        _currentPosition.value = exoPlayer.currentPosition
    }

    // Audio FX & Equalizer setup
    private fun setupAudioFx(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                }
                applyBandsToHardware()
            }
            if (bassBoost == null) {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    if (strengthSupported) {
                        setStrength(_bassBoostLevel.value)
                    }
                }
            }
            if (virtualizer == null) {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    if (strengthSupported) {
                        setStrength(_virtualizerLevel.value)
                    }
                }
            }
            if (presetReverb == null) {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    enabled = _eqEnabled.value
                    preset = _reverbPreset.value
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Audio FX: ${e.message}")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            presetReverb?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling EQ enabled: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Int, levelNormalized: Int) {
        val current = _bandLevels.value.clone()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelNormalized.coerceIn(-100, 100)
            _bandLevels.value = current
            applyBandsToHardware()
        }
    }

    private fun applyBandsToHardware() {
        val eq = equalizer ?: return
        try {
            val numHardwareBands = eq.numberOfBands.toInt()
            val minRange = eq.bandLevelRange[0]
            val maxRange = eq.bandLevelRange[1]
            val rangeDelta = maxRange - minRange

            for (i in 0 until numHardwareBands) {
                // Map 10 UI bands into available hardware bands
                val uiIndex = (i * 10 / numHardwareBands).coerceIn(0, 9)
                val norm = _bandLevels.value[uiIndex] // -100 to 100
                val hardwareLevel = minRange + ((norm + 100) * rangeDelta / 200)
                eq.setBandLevel(i.toShort(), hardwareLevel.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level: ${e.message}")
        }
    }

    fun setBassBoost(level: Short) {
        _bassBoostLevel.value = level
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(level)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(level: Short) {
        _virtualizerLevel.value = level
        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(level)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting virtualizer: ${e.message}")
        }
    }

    fun setReverbPreset(preset: Short) {
        _reverbPreset.value = preset
        try {
            presetReverb?.preset = preset
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reverb: ${e.message}")
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        stopProgressTracker()
        abandonAudioFocus()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        presetReverb?.release()
        exoPlayer.release()
    }
}
