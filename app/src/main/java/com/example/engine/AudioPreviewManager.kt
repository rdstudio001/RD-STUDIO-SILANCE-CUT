package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPreviewManager(private val context: Context) {

    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0,
        val durationMs: Long = 0,
        val playbackSpeed: Float = 1.0f,
        val volume: Float = 1.0f,
        val isLoaded: Boolean = false,
        val isProcessedMode: Boolean = false
    )

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentOriginalUri: Uri? = null
    private var currentProcessedFile: File? = null

    fun setMediaSources(originalUri: Uri?, processedFile: File?) {
        currentOriginalUri = originalUri
        currentProcessedFile = processedFile
        if (originalUri != null && !_state.value.isLoaded) {
            loadSource(useProcessed = false)
        }
    }

    fun switchMode(useProcessed: Boolean) {
        if (useProcessed && currentProcessedFile == null) return
        if (!useProcessed && currentOriginalUri == null) return

        val wasPlaying = _state.value.isPlaying
        val currentPos = _state.value.currentPositionMs

        loadSource(useProcessed)

        // Attempt to preserve relative seek position if valid
        mediaPlayer?.let { player ->
            if (currentPos in 0..player.duration) {
                player.seekTo(currentPos.toInt())
            }
            if (wasPlaying) {
                player.start()
                startProgressPolling()
            }
        }
    }

    private fun loadSource(useProcessed: Boolean) {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null

        val player = MediaPlayer()
        try {
            if (useProcessed && currentProcessedFile != null) {
                player.setDataSource(currentProcessedFile!!.absolutePath)
            } else if (currentOriginalUri != null) {
                player.setDataSource(context, currentOriginalUri!!)
            } else {
                return
            }

            player.prepare()
            val dur = player.duration.toLong()

            // Apply current playback speed and volume
            applyPlaybackParams(player, _state.value.playbackSpeed)
            player.setVolume(_state.value.volume, _state.value.volume)

            player.setOnCompletionListener {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentPositionMs = dur
                )
                stopProgressPolling()
            }

            mediaPlayer = player
            _state.value = _state.value.copy(
                isLoaded = true,
                durationMs = dur,
                currentPositionMs = 0,
                isPlaying = false,
                isProcessedMode = useProcessed
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoaded = false)
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            if (!player.isPlaying) {
                player.start()
                _state.value = _state.value.copy(isPlaying = true)
                startProgressPolling()
            }
        } catch (_: Exception) {}
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _state.value = _state.value.copy(isPlaying = false)
                stopProgressPolling()
            }
        } catch (_: Exception) {}
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    player.prepare()
                }
                player.seekTo(0)
            }
        } catch (_: Exception) {}
        stopProgressPolling()
        _state.value = _state.value.copy(
            isPlaying = false,
            currentPositionMs = 0
        )
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let { player ->
                val clamped = positionMs.coerceIn(0, player.duration.toLong()).toInt()
                player.seekTo(clamped)
                _state.value = _state.value.copy(currentPositionMs = clamped.toLong())
            }
        } catch (_: Exception) {}
    }

    fun setSpeed(speed: Float) {
        mediaPlayer?.let { applyPlaybackParams(it, speed) }
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
        _state.value = _state.value.copy(volume = clamped)
    }

    private fun applyPlaybackParams(player: MediaPlayer, speed: Float) {
        try {
            val params = player.playbackParams
            params.speed = speed
            player.playbackParams = params
        } catch (_: Exception) {}
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.value = _state.value.copy(
                            currentPositionMs = player.currentPosition.toLong(),
                            durationMs = player.duration.toLong()
                        )
                    }
                }
                delay(30)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
