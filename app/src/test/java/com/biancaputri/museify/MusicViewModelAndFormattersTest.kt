package com.biancaputri.museify

import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.JamendoHeadersDto
import com.biancaputri.museify.data.api.JamendoTrackDto
import com.biancaputri.museify.data.api.JamendoTracksResponse
import com.biancaputri.museify.data.repository.MusicError
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.ui.util.formatDuration
import com.biancaputri.museify.ui.util.greetingResForHour
import com.biancaputri.museify.viewmodel.MusicViewModel
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicViewModelAndFormattersTest {

    @Test
    fun formatDuration_formatsValidAndMissingDurations() {
        assertEquals("3:05", formatDuration(185))
        assertEquals("0:42", formatDuration(42))
        assertEquals("--:--", formatDuration(0))
        assertEquals("--:--", formatDuration(-10))
    }

    @Test
    fun greetingResForHour_mapsDayPeriodsCorrectly() {
        assertEquals(R.string.greeting_morning, greetingResForHour(8))
        assertEquals(R.string.greeting_afternoon, greetingResForHour(13))
        assertEquals(R.string.greeting_late_afternoon, greetingResForHour(16))
        assertEquals(R.string.greeting_evening, greetingResForHour(21))
        assertEquals(R.string.greeting_evening, greetingResForHour(2))
    }

    @Test
    fun musicViewModel_loadsHomeAndDebouncesSearch() = runBlocking {
        var searchApiCalls = 0
        val fakeApi = object : JamendoApi {
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
                    searchApiCalls++
                    return JamendoTracksResponse(
                        headers = JamendoHeadersDto(status = "success", code = 0),
                        results = listOf(
                            JamendoTrackDto(
                                id = "s-$searchApiCalls",
                                name = "Track for $search",
                                artistName = "Artist",
                                duration = 120
                            )
                        )
                    )
                }
                return JamendoTracksResponse(
                    headers = JamendoHeadersDto(status = "success", code = 0),
                    results = listOf(
                        JamendoTrackDto(
                            id = "h-${order ?: "default"}",
                            name = "Home Track ${order ?: ""}",
                            artistName = "Indie Artist",
                            duration = 195
                        )
                    )
                )
            }
        }

        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = MusicViewModel(
            repository = MusicRepository(fakeApi) { "valid-key" },
            ioDispatcher = Dispatchers.Unconfined,
            debounceMillis = 80L,
            loadInitialHome = false,
            externalScope = testScope
        )

        viewModel.loadHomeCatalog()
        delay(50L)

        val home = viewModel.homeState.value
        assertFalse(home.isLoading)
        assertNull(home.error)
        assertEquals(1, home.featuredSongs.size)
        assertEquals(1, home.recommendedSongs.size)
        assertEquals(1, home.curatedSongs.size)

        val picked = home.featuredSongs.first()
        viewModel.selectSong(picked)
        assertEquals(picked, viewModel.findSongById(picked.id))
        viewModel.playSong(picked)
        assertEquals(listOf(picked), viewModel.recentSongs.value)

        // Rapid keystrokes should debounce to a single API request
        viewModel.onSearchQueryChanged("j")
        viewModel.onSearchQueryChanged("ja")
        viewModel.onSearchQueryChanged("jazz")
        delay(200L)

        assertEquals(1, searchApiCalls)
        val search = viewModel.searchState.value
        assertTrue(search.hasSearched)
        assertFalse(search.isLoading)
        assertEquals("Track for jazz", search.results.first().title)

        viewModel.clearSearch()
        delay(50L)
        assertEquals("", viewModel.searchState.value.query)
        assertFalse(viewModel.searchState.value.hasSearched)
        assertTrue(viewModel.searchState.value.results.isEmpty())
        testScope.cancel()
    }

    @Test
    fun musicViewModel_mapsNetworkErrorOnHomeCatalog() = runBlocking {
        val offlineApi = object : JamendoApi {
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
                throw UnknownHostException("offline")
            }
        }

        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = MusicViewModel(
            repository = MusicRepository(offlineApi) { "valid-key" },
            ioDispatcher = Dispatchers.Unconfined,
            debounceMillis = 50L,
            loadInitialHome = false,
            externalScope = testScope
        )

        viewModel.loadHomeCatalog()
        delay(50L)

        val home = viewModel.homeState.value
        assertFalse(home.isLoading)
        assertEquals(MusicError.Network, home.error)
        testScope.cancel()
    }
}
