package com.biancaputri.museify.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.biancaputri.museify.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlaybackError {
    UNAVAILABLE_AUDIO,
    PLAYBACK_FAILED
}

data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: PlaybackError? = null
) {
    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex >= 0 && currentIndex < queue.lastIndex

    val effectiveDurationMs: Long
        get() = when {
            durationMs > 0L -> durationMs
            currentSong != null && currentSong.durationSeconds > 0 ->
                currentSong.durationSeconds * 1000L
            else -> 0L
        }
}

interface PlayerController {
    val playbackState: StateFlow<PlaybackUiState>
    fun playSong(song: Song, queue: List<Song> = listOf(song))
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun playNext()
    fun playPrevious()
    fun retryCurrentTrack()
    fun release()
}

/**
 * Shared Media3 ExoPlayer manager that handles audio streaming, queue navigation,
 * real-time playback position updates, and audio error handling.
 */
class MusicPlayerManager(context: Context) : PlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    override val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { state ->
                state.copy(
                    isPlaying = isPlaying,
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                )
            }
            if (isPlaying) {
                startProgressPolling()
            } else {
                stopProgressPolling()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val resolvedDuration = exoPlayer.duration
                .takeIf { it != C.TIME_UNSET && it > 0L }
                ?: _playbackState.value.effectiveDurationMs

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update {
                        it.copy(
                            isBuffering = true,
                            durationMs = resolvedDuration,
                            error = null
                        )
                    }
                }
                Player.STATE_READY -> {
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            durationMs = resolvedDuration,
                            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            error = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    stopProgressPolling()
                    if (_playbackState.value.hasNext) {
                        playNext()
                    } else {
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                positionMs = resolvedDuration
                            )
                        }
                    }
                }
                Player.STATE_IDLE -> {
                    stopProgressPolling()
                    _playbackState.update { it.copy(isBuffering = false) }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _playbackState.update {
                it.copy(positionMs = newPosition.positionMs.coerceAtLeast(0L))
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressPolling()
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    error = PlaybackError.PLAYBACK_FAILED
                )
            }
        }
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(playerListener)
        }

    override fun playSong(song: Song, queue: List<Song>) {
        val normalizedQueue = queue.ifEmpty { listOf(song) }
        val index = normalizedQueue.indexOfFirst { it.id == song.id }.let { idx ->
            if (idx >= 0) idx else 0
        }
        val activeQueue = if (normalizedQueue.any { it.id == song.id }) {
            normalizedQueue
        } else {
            listOf(song) + normalizedQueue
        }

        val audioUrl = song.audioUrl?.trim()
        if (audioUrl.isNullOrEmpty()) {
            stopProgressPolling()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _playbackState.value = PlaybackUiState(
                currentSong = song,
                queue = activeQueue,
                currentIndex = index,
                isPlaying = false,
                isBuffering = false,
                positionMs = 0L,
                durationMs = song.durationSeconds * 1000L,
                error = PlaybackError.UNAVAILABLE_AUDIO
            )
            return
        }

        _playbackState.value = PlaybackUiState(
            currentSong = song,
            queue = activeQueue,
            currentIndex = index,
            isPlaying = false,
            isBuffering = true,
            positionMs = 0L,
            durationMs = song.durationSeconds * 1000L,
            error = null
        )

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artistName)
        if (!song.albumName.isNullOrBlank()) {
            metadataBuilder.setAlbumTitle(song.albumName)
        }
        if (!song.artworkUrl.isNullOrBlank()) {
            runCatching { Uri.parse(song.artworkUrl) }
                .getOrNull()
                ?.let { metadataBuilder.setArtworkUri(it) }
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(audioUrl)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun togglePlayPause() {
        val state = _playbackState.value
        val current = state.currentSong ?: return
        if (!current.hasPlayableAudio) {
            _playbackState.update { it.copy(error = PlaybackError.UNAVAILABLE_AUDIO) }
            return
        }

        when {
            exoPlayer.playbackState == Player.STATE_ENDED -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            state.error == PlaybackError.PLAYBACK_FAILED -> {
                retryCurrentTrack()
            }
            exoPlayer.isPlaying -> {
                exoPlayer.pause()
            }
            else -> {
                exoPlayer.play()
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        val current = _playbackState.value.currentSong ?: return
        if (!current.hasPlayableAudio) return

        val maxDuration = _playbackState.value.effectiveDurationMs
        val clamped = if (maxDuration > 0L) {
            positionMs.coerceIn(0L, maxDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        exoPlayer.seekTo(clamped)
        _playbackState.update { it.copy(positionMs = clamped) }
    }

    override fun playNext() {
        val state = _playbackState.value
        if (!state.hasNext) return
        val nextIndex = state.currentIndex + 1
        playSong(state.queue[nextIndex], state.queue)
    }

    override fun playPrevious() {
        val state = _playbackState.value
        val current = state.currentSong ?: return
        val currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)

        if (currentPosition > RESTART_THRESHOLD_MS || !state.hasPrevious) {
            if (current.hasPlayableAudio) {
                exoPlayer.seekTo(0L)
                _playbackState.update { it.copy(positionMs = 0L) }
            }
            return
        }

        val previousIndex = state.currentIndex - 1
        playSong(state.queue[previousIndex], state.queue)
    }

    override fun retryCurrentTrack() {
        val state = _playbackState.value
        val current = state.currentSong ?: return
        playSong(current, state.queue)
    }

    override fun release() {
        stopProgressPolling()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        scope.cancel()
    }

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val resolvedDuration = exoPlayer.duration
                    .takeIf { it != C.TIME_UNSET && it > 0L }
                    ?: _playbackState.value.effectiveDurationMs

                _playbackState.update {
                    it.copy(
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = resolvedDuration
                    )
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private companion object {
        const val PROGRESS_POLL_INTERVAL_MS = 250L
        const val RESTART_THRESHOLD_MS = 3000L
    }
}
