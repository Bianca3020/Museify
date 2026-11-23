package com.biancaputri.museify.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.biancaputri.museify.data.local.MuseifyDatabase
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.data.repository.FavoritesRepository
import com.biancaputri.museify.data.repository.FavoritesStore
import com.biancaputri.museify.data.repository.MusicError
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.data.repository.MusicResult
import com.biancaputri.museify.player.MusicPlayerManager
import com.biancaputri.museify.player.PlaybackUiState
import com.biancaputri.museify.player.PlayerController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = true,
    val featuredSongs: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val curatedSongs: List<Song> = emptyList(),
    val error: MusicError? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null &&
            featuredSongs.isEmpty() && recommendedSongs.isEmpty() && curatedSongs.isEmpty()
}

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Song> = emptyList(),
    val error: MusicError? = null,
    val hasSearched: Boolean = false
) {
    val isEmpty: Boolean
        get() = hasSearched && !isLoading && error == null && results.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MusicViewModel(
    private val repository: MusicRepository = MusicRepository(),
    private val playerController: PlayerController? = null,
    private val favoritesStore: FavoritesStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    debounceMillis: Long = 400L,
    loadInitialHome: Boolean = true,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    @Suppress("unused")
    constructor() : this(
        repository = MusicRepository(),
        playerController = null,
        favoritesStore = null,
        ioDispatcher = Dispatchers.IO,
        debounceMillis = 400L,
        loadInitialHome = true,
        externalScope = null
    )

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs: StateFlow<List<Song>> = _recentSongs.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteError = MutableStateFlow(false)
    val favoriteError: StateFlow<Boolean> = _favoriteError.asStateFlow()

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    private val fallbackPlaybackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState>
        get() = playerController?.playbackState ?: fallbackPlaybackState.asStateFlow()

    private val searchTriggerFlow = MutableStateFlow(SearchTrigger(query = ""))
    private val songCache = LinkedHashMap<String, Song>()

    init {
        observeDebouncedSearch(debounceMillis)
        observeActivePlaybackTrack()
        observeFavorites()
        if (loadInitialHome) {
            loadHomeCatalog()
        }
    }

    fun loadHomeCatalog() {
        scope.launch {
            _homeState.update { it.copy(isLoading = true, error = null) }

            val featuredDeferred = async(ioDispatcher) { repository.getFeaturedTracks() }
            val recommendedDeferred = async(ioDispatcher) { repository.getRecommendedTracks() }
            val curatedDeferred = async(ioDispatcher) { repository.getCuratedTracks() }

            val featuredResult = featuredDeferred.await()
            val recommendedResult = recommendedDeferred.await()
            val curatedResult = curatedDeferred.await()

            val featured = (featuredResult as? MusicResult.Success)?.data.orEmpty()
            val recommended = (recommendedResult as? MusicResult.Success)?.data.orEmpty()
            val curated = (curatedResult as? MusicResult.Success)?.data.orEmpty()

            cacheSongs(featured)
            cacheSongs(recommended)
            cacheSongs(curated)

            val firstError = listOf(featuredResult, recommendedResult, curatedResult)
                .filterIsInstance<MusicResult.Error>()
                .firstOrNull()
                ?.error

            val hasAnyContent = featured.isNotEmpty() || recommended.isNotEmpty() || curated.isNotEmpty()

            _homeState.value = HomeUiState(
                isLoading = false,
                featuredSongs = featured,
                recommendedSongs = recommended,
                curatedSongs = curated,
                error = if (hasAnyContent) null else firstError
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.update { current ->
            if (query.isBlank()) {
                current.copy(
                    query = query,
                    isLoading = false,
                    results = emptyList(),
                    error = null,
                    hasSearched = false
                )
            } else {
                current.copy(
                    query = query,
                    error = null
                )
            }
        }
        searchTriggerFlow.value = SearchTrigger(
            query = query,
            immediate = false,
            nonce = searchTriggerFlow.value.nonce
        )
    }

    fun submitSearch() {
        val currentQuery = _searchState.value.query
        if (currentQuery.isBlank()) return
        searchTriggerFlow.value = SearchTrigger(
            query = currentQuery,
            immediate = true,
            nonce = searchTriggerFlow.value.nonce + 1
        )
    }

    fun retrySearch() {
        submitSearch()
    }

    fun clearSearch() {
        onSearchQueryChanged("")
    }

    fun selectSong(song: Song) {
        cacheSongs(listOf(song))
        _selectedSong.value = song
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        cacheSongs(queue)
        selectSong(song)
        recordRecentSong(song)
        playerController?.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerController?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerController?.seekTo(positionMs)
    }

    fun playNext() {
        playerController?.playNext()
    }

    fun playPrevious() {
        playerController?.playPrevious()
    }

    fun retryPlayback() {
        playerController?.retryCurrentTrack()
    }

    fun toggleFavorite(song: Song) {
        val store = favoritesStore ?: return
        cacheSongs(listOf(song))
        scope.launch {
            val result = withContext(ioDispatcher) {
                store.toggleFavorite(song)
            }
            if (result.isFailure) {
                _favoriteError.value = true
            }
        }
    }

    fun dismissFavoriteError() {
        _favoriteError.value = false
    }

    fun findSongById(songId: String): Song? {
        return playbackState.value.currentSong?.takeIf { it.id == songId }
            ?: _selectedSong.value?.takeIf { it.id == songId }
            ?: _favoriteSongs.value.firstOrNull { it.id == songId }
            ?: songCache[songId]
    }

    override fun onCleared() {
        playerController?.release()
        super.onCleared()
    }

    private fun observeFavorites() {
        val store = favoritesStore ?: return
        scope.launch {
            store.observeFavorites()
                .catch { _favoriteError.value = true }
                .collect { songs ->
                    cacheSongs(songs)
                    _favoriteSongs.value = songs
                }
        }
        scope.launch {
            store.observeFavoriteIds()
                .catch { _favoriteError.value = true }
                .collect { ids ->
                    _favoriteIds.value = ids
                }
        }
    }

    private fun observeActivePlaybackTrack() {
        val controller = playerController ?: return
        scope.launch {
            controller.playbackState
                .map { it.currentSong }
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.id == new.id }
                .collect { song ->
                    cacheSongs(listOf(song))
                    recordRecentSong(song)
                }
        }
    }

    private fun recordRecentSong(song: Song) {
        _recentSongs.update { existing ->
            (listOf(song) + existing.filterNot { it.id == song.id }).take(10)
        }
    }

    private fun observeDebouncedSearch(debounceMillis: Long) {
        scope.launch {
            searchTriggerFlow
                .debounce { trigger ->
                    if (trigger.query.isBlank() || trigger.immediate) 0L else debounceMillis
                }
                .distinctUntilChanged { old, new ->
                    old.query.trim() == new.query.trim() && old.nonce == new.nonce
                }
                .flatMapLatest { trigger ->
                    flow {
                        val trimmed = trigger.query.trim()
                        if (trimmed.isEmpty()) {
                            emit(SearchEmission.Cleared)
                        } else {
                            emit(SearchEmission.Loading)
                            when (val result = repository.searchTracks(trimmed)) {
                                is MusicResult.Success -> {
                                    cacheSongs(result.data)
                                    emit(SearchEmission.Loaded(result.data))
                                }
                                is MusicResult.Error -> {
                                    emit(SearchEmission.Failed(result.error))
                                }
                            }
                        }
                    }.flowOn(ioDispatcher)
                }
                .collectLatest { emission ->
                    _searchState.update { state ->
                        when (emission) {
                            SearchEmission.Cleared -> state.copy(
                                isLoading = false,
                                results = emptyList(),
                                error = null,
                                hasSearched = false
                            )
                            SearchEmission.Loading -> state.copy(
                                isLoading = true,
                                error = null
                            )
                            is SearchEmission.Loaded -> state.copy(
                                isLoading = false,
                                results = emission.songs,
                                error = null,
                                hasSearched = true
                            )
                            is SearchEmission.Failed -> state.copy(
                                isLoading = false,
                                results = emptyList(),
                                error = emission.error,
                                hasSearched = true
                            )
                        }
                    }
                }
        }
    }

    private fun cacheSongs(songs: List<Song>) {
        songs.forEach { song ->
            songCache[song.id] = song
        }
    }

    private data class SearchTrigger(
        val query: String,
        val immediate: Boolean = false,
        val nonce: Int = 0
    )

    private sealed interface SearchEmission {
        data object Cleared : SearchEmission
        data object Loading : SearchEmission
        data class Loaded(val songs: List<Song>) : SearchEmission
        data class Failed(val error: MusicError) : SearchEmission
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = MuseifyDatabase.getInstance(appContext)
                    return MusicViewModel(
                        repository = MusicRepository(),
                        playerController = MusicPlayerManager(appContext),
                        favoritesStore = FavoritesRepository(database.favoriteDao())
                    ) as T
                }
            }
        }
    }
}
