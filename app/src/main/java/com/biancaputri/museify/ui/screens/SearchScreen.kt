package com.biancaputri.museify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.R
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.ui.components.EmptyStateView
import com.biancaputri.museify.ui.components.ErrorStateView
import com.biancaputri.museify.ui.components.LoadingState
import com.biancaputri.museify.ui.components.MuseifySearchBar
import com.biancaputri.museify.ui.components.SongListItem
import com.biancaputri.museify.ui.components.SongRowActions
import com.biancaputri.museify.ui.util.computeAdaptiveLayoutSpec
import com.biancaputri.museify.ui.util.toStringRes
import com.biancaputri.museify.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    favoriteIds: Set<String>,
    activeSongId: String?,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onSongDetails: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutSpec = computeAdaptiveLayoutSpec(
            widthDp = maxWidth.value.toInt(),
            heightDp = maxHeight.value.toInt()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layoutSpec.horizontalPadding)
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(14.dp))
            MuseifySearchBar(
                query = searchState.query,
                onQueryChange = onQueryChange,
                onSearchSubmit = onSearchSubmit,
                onClear = onClearSearch
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    searchState.query.isBlank() && !searchState.hasSearched -> {
                        EmptyStateView(
                            title = stringResource(R.string.search_initial_title),
                            message = stringResource(R.string.search_initial_subtitle),
                            icon = Icons.Rounded.Search,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    searchState.isLoading -> {
                        LoadingState(modifier = Modifier.align(Alignment.Center))
                    }
                    searchState.error != null -> {
                        ErrorStateView(
                            message = stringResource(searchState.error.toStringRes()),
                            onRetry = onRetry,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    searchState.isEmpty -> {
                        EmptyStateView(
                            title = stringResource(R.string.search_empty_title),
                            message = stringResource(R.string.search_empty_message),
                            icon = Icons.Rounded.SearchOff,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    layoutSpec.listColumns > 1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(layoutSpec.listColumns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(layoutSpec.listColumns) }) {
                                Text(
                                    text = stringResource(
                                        R.string.search_results_count,
                                        searchState.results.size
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(
                                items = searchState.results,
                                key = { "search_${it.id}" }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, searchState.results) },
                                    trailingContent = {
                                        SongRowActions(
                                            songTitle = song.title,
                                            isFavorite = song.id in favoriteIds,
                                            onToggleFavorite = { onToggleFavorite(song) },
                                            onOpenDetails = { onSongDetails(song) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "results_header") {
                                Text(
                                    text = stringResource(
                                        R.string.search_results_count,
                                        searchState.results.size
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(
                                items = searchState.results,
                                key = { "search_${it.id}" }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, searchState.results) },
                                    trailingContent = {
                                        SongRowActions(
                                            songTitle = song.title,
                                            isFavorite = song.id in favoriteIds,
                                            onToggleFavorite = { onToggleFavorite(song) },
                                            onOpenDetails = { onSongDetails(song) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
