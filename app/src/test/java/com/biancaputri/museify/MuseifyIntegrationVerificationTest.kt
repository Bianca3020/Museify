package com.biancaputri.museify

import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.JamendoHeadersDto
import com.biancaputri.museify.data.api.JamendoTrackDto
import com.biancaputri.museify.data.api.JamendoTracksResponse
import com.biancaputri.museify.data.local.FavoriteDao
import com.biancaputri.museify.data.local.FavoriteEntity
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.data.repository.FavoritesRepository
import com.biancaputri.museify.data.repository.MusicError
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.data.repository.MusicResult
import com.biancaputri.museify.player.PlaybackUiState
import com.biancaputri.museify.player.PlayerController
import com.biancaputri.museify.viewmodel.MusicViewModel
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuseifyIntegrationVerificationTest {

    @Test
    fun musicRepository_deduplicatesDuplicateTrackIdsFromApi() = runBlocking {
        val duplicateApi = object : JamendoApi {
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
            ): JamendoTracksResponse {
                return JamendoTracksResponse(
                    headers = JamendoHeadersDto(status = "success", code = 0),
                    results = listOf(
                        JamendoTrackDto(id = "101", name = "First Copy", artistName = "Artist A"),
                        JamendoTrackDto(id = "101", name = "Duplicate Copy", artistName = "Artist A"),
                        JamendoTrackDto(id = "102", name = "Second Track", artistName = "Artist B")
                    )
                )
            }
        }

        val result = MusicRepository(duplicateApi) { "client-id" }.getCuratedTracks()
        assertTrue(result is MusicResult.Success)
        val songs = (result as MusicResult.Success).data
        assertEquals(listOf("101", "102"), songs.map { it.id })
        assertEquals("First Copy", songs.first().title)
    }

    @Test
    fun endToEndFlow_catalogPlaybackFavoritesSearchAndRetryWorkTogether() = runBlocking {
        var failSearchOnce = true
        val api = object : JamendoApi {
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
            ): JamendoTracksResponse {
                if (!search.isNullOrBlank()) {
                    if (failSearchOnce) {
                        failSearchOnce = false
                        throw IOException("Transient error")
                    }
                    return JamendoTracksResponse(
                        headers = JamendoHeadersDto(status = "success", code = 0),
                        results = listOf(
                            JamendoTrackDto(
                                id = "900",
                                name = "Ocean Breeze",
                                artistName = "Calm Wave",
                                audio = "https://stream.example/900.mp3",
                                duration = 160
                            )
                        )
                    )
                }
                return JamendoTracksResponse(
                    headers = JamendoHeadersDto(status = "success", code = 0),
                    results = listOf(
                        JamendoTrackDto(
                            id = "1",
                            name = "Aurora",
                            artistName = "Lumen",
                            audio = "https://stream.example/1.mp3",
                            duration = 200
                        ),
                        JamendoTrackDto(
                            id = "2",
                            name = "Echoes",
                            artistName = "Vesper",
                            audio = "https://stream.example/2.mp3",
                            duration = 180
                        )
                    )
                )
            }
        }

        val dao = InMemoryFavoriteDao()
        val player = RecordingPlayerController()
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = MusicViewModel(
            repository = MusicRepository(api) { "client-id" },
            playerController = player,
            favoritesStore = FavoritesRepository(dao),
            ioDispatcher = Dispatchers.Unconfined,
            debounceMillis = 40L,
            loadInitialHome = false,
            externalScope = testScope
        )

        // 1. Load home catalog
        viewModel.loadHomeCatalog()
        delay(30L)
        assertEquals(2, viewModel.homeState.value.featuredSongs.size)

        // 2. Play and favorite first track
        val firstTrack = viewModel.homeState.value.featuredSongs.first()
        viewModel.playSong(firstTrack, viewModel.homeState.value.featuredSongs)
        viewModel.toggleFavorite(firstTrack)
        assertTrue(viewModel.playbackState.value.isPlaying)
        assertEquals(setOf("1"), viewModel.favoriteIds.value)
        assertEquals(firstTrack, viewModel.findSongById("1"))

        // 3. Pause, seek, next
        viewModel.togglePlayPause()
        assertFalse(viewModel.playbackState.value.isPlaying)
        viewModel.seekTo(30_000L)
        assertEquals(30_000L, viewModel.playbackState.value.positionMs)
        viewModel.playNext()
        assertEquals("2", viewModel.playbackState.value.currentSong?.id)

        // 4. Search fails then succeeds on retry
        viewModel.onSearchQueryChanged("ocean")
        delay(100L)
        assertEquals(MusicError.Network, viewModel.searchState.value.error)

        viewModel.retrySearch()
        delay(50L)
        assertNull(viewModel.searchState.value.error)
        assertEquals(1, viewModel.searchState.value.results.size)
        assertEquals("Ocean Breeze", viewModel.searchState.value.results.first().title)

        testScope.cancel()
    }

    private class InMemoryFavoriteDao : FavoriteDao {
        private val state = MutableStateFlow<List<FavoriteEntity>>(emptyList())

        override fun observeFavorites(): Flow<List<FavoriteEntity>> = state

        override fun observeFavoriteIds(): Flow<List<String>> = state.map { it.map(FavoriteEntity::id) }

        override suspend fun upsertFavorite(favorite: FavoriteEntity) {
            state.value = listOf(favorite) + state.value.filterNot { it.id == favorite.id }
        }

        override suspend fun deleteFavoriteById(songId: String) {
            state.value = state.value.filterNot { it.id == songId }
        }

        override suspend fun isFavorite(songId: String): Boolean = state.value.any { it.id == songId }
    }

    private class RecordingPlayerController : PlayerController {
        private val _state = MutableStateFlow(PlaybackUiState())
        override val playbackState: StateFlow<PlaybackUiState> = _state.asStateFlow()

        override fun playSong(song: Song, queue: List<Song>) {
            val idx = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            _state.value = PlaybackUiState(
                currentSong = song,
                queue = queue,
                currentIndex = idx,
                isPlaying = true,
                durationMs = song.durationSeconds * 1000L
            )
        }

        override fun togglePlayPause() {
            _state.update { it.copy(isPlaying = !it.isPlaying) }
        }

        override fun seekTo(positionMs: Long) {
            _state.update { it.copy(positionMs = positionMs) }
        }

        override fun playNext() {
            val s = _state.value
            if (s.hasNext) playSong(s.queue[s.currentIndex + 1], s.queue)
        }

        override fun playPrevious() {
            val s = _state.value
            if (s.hasPrevious) playSong(s.queue[s.currentIndex - 1], s.queue)
        }

        override fun retryCurrentTrack() = Unit
        override fun release() = Unit
    }
}
