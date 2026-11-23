package com.biancaputri.museify.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.biancaputri.museify.ui.components.MuseifyBottomBar
import com.biancaputri.museify.ui.components.MuseifyTopLevelDestination
import com.biancaputri.museify.ui.screens.HomeScreen
import com.biancaputri.museify.ui.screens.SearchScreen
import com.biancaputri.museify.ui.screens.SongDetailsScreen
import com.biancaputri.museify.viewmodel.MusicViewModel

private object MuseifyRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SONG_DETAILS = "song_details/{songId}"

    fun songDetails(songId: String): String = "song_details/${Uri.encode(songId)}"
}

@Composable
fun MuseifyNavHost(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val homeState by musicViewModel.homeState.collectAsState()
    val searchState by musicViewModel.searchState.collectAsState()
    val recentSongs by musicViewModel.recentSongs.collectAsState()

    val showBottomBar = MuseifyTopLevelDestination.entries.any { it.route == currentRoute }

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
        bottomBar = {
            if (showBottomBar) {
                MuseifyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = navigateToTopLevel
                )
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
                    onSearchClick = { navigateToTopLevel(MuseifyTopLevelDestination.SEARCH) },
                    onSongClick = { song ->
                        musicViewModel.selectSong(song)
                        navController.navigate(MuseifyRoutes.songDetails(song.id))
                    },
                    onRetry = { musicViewModel.loadHomeCatalog() }
                )
            }

            composable(MuseifyRoutes.SEARCH) {
                SearchScreen(
                    searchState = searchState,
                    onQueryChange = musicViewModel::onSearchQueryChanged,
                    onSearchSubmit = musicViewModel::submitSearch,
                    onClearSearch = musicViewModel::clearSearch,
                    onRetry = musicViewModel::retrySearch,
                    onSongClick = { song ->
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
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
