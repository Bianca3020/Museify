package com.biancaputri.museify

import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.JamendoTracksResponse
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.player.PlaybackError
import com.biancaputri.museify.player.PlaybackUiState
import com.biancaputri.museify.player.PlayerController
import com.biancaputri.museify.ui.util.formatDurationMillis
import com.biancaputri.museify.viewmodel.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlayerAndPlaybackTest {

    private fun sampleSong(id: String, audioUrl: String? = "https://stream.example/$id.mp3"): Song {
        return Song(
            id = id,
            title = "Track $id",
            artistName = "Artist $id",
            albumName = "Album",
            artworkUrl = null,
            audioUrl = audioUrl,
            durationSeconds = 180,
            shareUrl = null,
            licenseUrl = null,
            licenseSummary = null
        )
    }

    @Test
    fun formatDurationMillis_formatsExpectedTimestamps() {
        assertEquals("0:00", formatDurationMillis(0L))
        assertEquals("0:00", formatDurationMillis(-500L))
        assertEquals("0:09", formatDurationMillis(9_500L))
        assertEquals("3:05", formatDurationMillis(185_000L))
    }

    @Test
    fun playbackUiState_computesQueueFlagsAndEffectiveDuration() {
        val songA = sampleSong("1")
        val songB = sampleSong("2")
        val state = PlaybackUiState(
            currentSong = songA,
            queue = listOf(songA, songB),
            currentIndex = 0,
            durationMs = 0L
        )
        assertFalse(state.hasPrevious)
        assertTrue(state.hasNext)
        assertEquals(180_000L, state.effectiveDurationMs)

        val readyState = state.copy(currentIndex = 1, durationMs = 192_500L)
        assertTrue(readyState.hasPrevious)
        assertFalse(readyState.hasNext)
        assertEquals(192_500L, readyState.effectiveDurationMs)
    }

    @Test
    fun musicViewModel_delegatesPlaybackControlsAndUpdatesRecentSongs() {
        val fakePlayer = FakePlayerController()
        val dummyApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse = JamendoTracksResponse()
        }
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = MusicViewModel(
            repository = MusicRepository(dummyApi) { "key" },
            playerController = fakePlayer,
            ioDispatcher = Dispatchers.Unconfined,
            loadInitialHome = false,
            externalScope = testScope
        )

        val song1 = sampleSong("1")
        val song2 = sampleSong("2")
        val unplayable = sampleSong("3", audioUrl = null)

        viewModel.playSong(song1, listOf(song1, song2))
        assertTrue(viewModel.playbackState.value.isPlaying)
        assertEquals("1", viewModel.playbackState.value.currentSong?.id)
        assertEquals(listOf(song1), viewModel.recentSongs.value)

        viewModel.togglePlayPause()
        assertFalse(viewModel.playbackState.value.isPlaying)

        viewModel.seekTo(45_000L)
        assertEquals(45_000L, viewModel.playbackState.value.positionMs)

        viewModel.playNext()
        assertEquals("2", viewModel.playbackState.value.currentSong?.id)
        assertEquals(listOf(song2, song1), viewModel.recentSongs.value)

        viewModel.playPrevious()
        assertEquals("1", viewModel.playbackState.value.currentSong?.id)

        viewModel.playSong(unplayable)
        assertEquals(PlaybackError.UNAVAILABLE_AUDIO, viewModel.playbackState.value.error)
        assertFalse(viewModel.playbackState.value.isPlaying)

        testScope.cancel()
    }

    private class FakePlayerController : PlayerController {
        private val _state = MutableStateFlow(PlaybackUiState())
        override val playbackState: StateFlow<PlaybackUiState> = _state.asStateFlow()

        override fun playSong(song: Song, queue: List<Song>) {
            val activeQueue = queue.ifEmpty { listOf(song) }
            val index = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            if (!song.hasPlayableAudio) {
                _state.value = PlaybackUiState(
                    currentSong = song,
                    queue = activeQueue,
                    currentIndex = index,
                    isPlaying = false,
                    error = PlaybackError.UNAVAILABLE_AUDIO
                )
            } else {
                _state.value = PlaybackUiState(
                    currentSong = song,
                    queue = activeQueue,
                    currentIndex = index,
                    isPlaying = true,
                    durationMs = song.durationSeconds * 1000L,
                    error = null
                )
            }
        }

        override fun togglePlayPause() {
            _state.update { it.copy(isPlaying = !it.isPlaying) }
        }

        override fun seekTo(positionMs: Long) {
            _state.update { it.copy(positionMs = positionMs.coerceIn(0L, it.effectiveDurationMs)) }
        }

        override fun playNext() {
            val current = _state.value
            if (current.hasNext) {
                playSong(current.queue[current.currentIndex + 1], current.queue)
            }
        }

        override fun playPrevious() {
            val current = _state.value
            if (current.hasPrevious) {
                playSong(current.queue[current.currentIndex - 1], current.queue)
            }
        }

        override fun retryCurrentTrack() {
            _state.value.currentSong?.let { playSong(it, _state.value.queue) }
        }

        override fun release() {
            // No-op in fake
        }
    }
}
