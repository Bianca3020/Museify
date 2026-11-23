package com.biancaputri.museify.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.biancaputri.museify.R
import com.biancaputri.museify.ui.components.MiniPlayer
import com.biancaputri.museify.ui.components.MuseifyBottomBar
import com.biancaputri.museify.ui.components.MuseifyTopLevelDestination
import com.biancaputri.museify.ui.screens.FavoritesScreen
import com.biancaputri.museify.ui.screens.HomeScreen
import com.biancaputri.museify.ui.screens.NowPlayingScreen
import com.biancaputri.museify.ui.screens.SearchScreen
import com.biancaputri.museify.ui.screens.SettingsScreen
import com.biancaputri.museify.ui.screens.SongDetailsScreen
import com.biancaputri.museify.viewmodel.MusicViewModel
import com.biancaputri.museify.viewmodel.SettingsViewModel

private object MuseifyRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val SONG_DETAILS = "song_details/{songId}"

    fun songDetails(songId: String): String = "song_details/${Uri.encode(songId)}"
}

@Composable
fun MuseifyNavHost(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val musicViewModel: MusicViewModel = viewModel(
        factory = MusicViewModel.factory(context)
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val homeState by musicViewModel.homeState.collectAsState()
    val searchState by musicViewModel.searchState.collectAsState()
    val recentSongs by musicViewModel.recentSongs.collectAsState()
    val favoriteSongs by musicViewModel.favoriteSongs.collectAsState()
    val favoriteIds by musicViewModel.favoriteIds.collectAsState()
    val favoriteError by musicViewModel.favoriteError.collectAsState()
    val playbackState by musicViewModel.playbackState.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val appLanguage by settingsViewModel.appLanguage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val dbErrorMessage = stringResource(R.string.error_database)

    LaunchedEffect(favoriteError) {
        if (favoriteError) {
            snackbarHostState.showSnackbar(dbErrorMessage)
            musicViewModel.dismissFavoriteError()
        }
    }

    val showBottomBar = MuseifyTopLevelDestination.entries.any { it.route == currentRoute }
    val showMiniPlayer = showBottomBar && playbackState.currentSong != null

    val navigateToTopLevel: (MuseifyTopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar || showMiniPlayer) {
                Column {
                    if (showMiniPlayer) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onPlayPauseClick = musicViewModel::togglePlayPause,
                            onClick = { navController.navigate(MuseifyRoutes.NOW_PLAYING) }
                        )
                    }
                    if (showBottomBar) {
                        MuseifyBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateToTopLevel
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MuseifyRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(MuseifyRoutes.HOME) {
                HomeScreen(
                    homeState = homeState,
                    recentSongs = recentSongs,
                    favoriteIds = favoriteIds,
                    activeSongId = playbackState.currentSong?.id,
                    onSearchClick = { navigateToTopLevel(MuseifyTopLevelDestination.SEARCH) },
                    onSettingsClick = { navController.navigate(MuseifyRoutes.SETTINGS) },
                    onSongPlay = { song, queue ->
                        musicViewModel.playSong(song, queue)
                    },
                    onSongDetails = { song ->
                        musicViewModel.selectSong(song)
                        navController.navigate(MuseifyRoutes.songDetails(song.id))
                    },
                    onToggleFavorite = musicViewModel::toggleFavorite,
                    onRetry = { musicViewModel.loadHomeCatalog() }
                )
            }

            composable(MuseifyRoutes.SEARCH) {
                SearchScreen(
                    searchState = searchState,
                    favoriteIds = favoriteIds,
                    activeSongId = playbackState.currentSong?.id,
                    onQueryChange = musicViewModel::onSearchQueryChanged,
                    onSearchSubmit = musicViewModel::submitSearch,
                    onClearSearch = musicViewModel::clearSearch,
                    onRetry = musicViewModel::retrySearch,
                    onSongPlay = { song, queue ->
                        musicViewModel.playSong(song, queue)
                    },
                    onSongDetails = { song ->
                        musicViewModel.selectSong(song)
                        navController.navigate(MuseifyRoutes.songDetails(song.id))
                    },
                    onToggleFavorite = musicViewModel::toggleFavorite
                )
            }

            composable(MuseifyRoutes.FAVORITES) {
                FavoritesScreen(
                    favoriteSongs = favoriteSongs,
                    activeSongId = playbackState.currentSong?.id,
                    onSongPlay = { song, queue ->
                        musicViewModel.playSong(song, queue)
                    },
                    onSongDetails = { song ->
                        musicViewModel.selectSong(song)
                        navController.navigate(MuseifyRoutes.songDetails(song.id))
                    },
                    onToggleFavorite = musicViewModel::toggleFavorite
                )
            }

            composable(MuseifyRoutes.SETTINGS) {
                SettingsScreen(
                    currentThemeMode = themeMode,
                    currentLanguage = appLanguage,
                    onThemeModeSelected = settingsViewModel::setThemeMode,
                    onLanguageSelected = settingsViewModel::setLanguage,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(MuseifyRoutes.NOW_PLAYING) {
                val currentId = playbackState.currentSong?.id
                NowPlayingScreen(
                    playbackState = playbackState,
                    isFavorite = currentId != null && currentId in favoriteIds,
                    onBack = { navController.popBackStack() },
                    onPlayPause = musicViewModel::togglePlayPause,
                    onSeek = musicViewModel::seekTo,
                    onNext = musicViewModel::playNext,
                    onPrevious = musicViewModel::playPrevious,
                    onRetry = musicViewModel::retryPlayback,
                    onToggleFavorite = musicViewModel::toggleFavorite,
                    onOpenDetails = { song ->
                        musicViewModel.selectSong(song)
                        navController.navigate(MuseifyRoutes.songDetails(song.id))
                    }
                )
            }

            composable(
                route = MuseifyRoutes.SONG_DETAILS,
                arguments = listOf(navArgument("songId") { type = NavType.StringType })
            ) { entry ->
                val songId = entry.arguments?.getString("songId").orEmpty()
                val song = musicViewModel.findSongById(songId)
                SongDetailsScreen(
                    song = song,
                    isFavorite = song != null && song.id in favoriteIds,
                    onBack = { navController.popBackStack() },
                    onPlayClick = { target ->
                        musicViewModel.playSong(target, listOf(target))
                        navController.navigate(MuseifyRoutes.NOW_PLAYING)
                    },
                    onToggleFavorite = musicViewModel::toggleFavorite
                )
            }
        }
    }
}
