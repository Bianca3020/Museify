package com.biancaputri.museify

import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.JamendoTracksResponse
import com.biancaputri.museify.data.local.FavoriteDao
import com.biancaputri.museify.data.local.FavoriteEntity
import com.biancaputri.museify.data.local.toFavoriteEntity
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.data.repository.FavoritesRepository
import com.biancaputri.museify.data.repository.FavoritesStore
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.viewmodel.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesRepositoryAndViewModelTest {

    private fun sampleSong(id: String): Song {
        return Song(
            id = id,
            title = "Favorite $id",
            artistName = "Artist $id",
            albumName = "Album $id",
            artworkUrl = "https://img.example/$id.jpg",
            audioUrl = "https://stream.example/$id.mp3",
            durationSeconds = 210,
            shareUrl = "https://jamendo.com/track/$id",
            licenseUrl = "https://creativecommons.org/licenses/by/3.0/",
            licenseSummary = "CC BY 3.0"
        )
    }

    @Test
    fun favoriteEntity_roundTripPreservesAllSongMetadata() {
        val original = sampleSong("42")
        val roundTripped = original.toFavoriteEntity(addedAt = 123456L).toSong()
        assertEquals(original, roundTripped)
    }

    @Test
    fun favoritesRepository_togglesAndObservesFavorites() = runBlocking {
        val fakeDao = FakeFavoriteDao()
        val repository = FavoritesRepository(fakeDao)
        val song = sampleSong("99")

        val added = repository.toggleFavorite(song)
        assertTrue(added.isSuccess)
        assertEquals(true, added.getOrNull())
        assertEquals(listOf(song), repository.observeFavorites().first())
        assertEquals(setOf("99"), repository.observeFavoriteIds().first())

        val removed = repository.toggleFavorite(song)
        assertTrue(removed.isSuccess)
        assertEquals(false, removed.getOrNull())
        assertTrue(repository.observeFavorites().first().isEmpty())
        assertTrue(repository.observeFavoriteIds().first().isEmpty())
    }

    @Test
    fun musicViewModel_syncsFavoritesAndReportsErrorsSafely() = runBlocking {
        val fakeDao = FakeFavoriteDao()
        val store: FavoritesStore = FavoritesRepository(fakeDao)
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
            favoritesStore = store,
            ioDispatcher = Dispatchers.Unconfined,
            loadInitialHome = false,
            externalScope = testScope
        )

        val song = sampleSong("7")
        viewModel.toggleFavorite(song)
        assertEquals(setOf("7"), viewModel.favoriteIds.value)
        assertEquals(listOf(song), viewModel.favoriteSongs.value)
        assertEquals(song, viewModel.findSongById("7"))

        fakeDao.shouldFailWrite = true
        viewModel.toggleFavorite(sampleSong("8"))
        assertTrue(viewModel.favoriteError.value)
        viewModel.dismissFavoriteError()
        assertFalse(viewModel.favoriteError.value)

        testScope.cancel()
    }

    private class FakeFavoriteDao : FavoriteDao {
        private val items = MutableStateFlow<List<FavoriteEntity>>(emptyList())
        var shouldFailWrite: Boolean = false

        override fun observeFavorites(): Flow<List<FavoriteEntity>> = items

        override fun observeFavoriteIds(): Flow<List<String>> {
            return items.map { list -> list.map { it.id } }
        }

        override suspend fun upsertFavorite(favorite: FavoriteEntity) {
            if (shouldFailWrite) error("Simulated DB write failure")
            items.value = listOf(favorite) + items.value.filterNot { it.id == favorite.id }
        }

        override suspend fun deleteFavoriteById(songId: String) {
            if (shouldFailWrite) error("Simulated DB delete failure")
            items.value = items.value.filterNot { it.id == songId }
        }

        override suspend fun isFavorite(songId: String): Boolean {
            return items.value.any { it.id == songId }
        }
    }
}
