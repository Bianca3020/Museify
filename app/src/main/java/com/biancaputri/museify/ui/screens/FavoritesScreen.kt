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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import com.biancaputri.museify.ui.components.SongListItem
import com.biancaputri.museify.ui.components.SongRowActions
import com.biancaputri.museify.ui.util.computeAdaptiveLayoutSpec

@Composable
fun FavoritesScreen(
    favoriteSongs: List<Song>,
    activeSongId: String?,
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
                text = stringResource(R.string.favorites_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            if (favoriteSongs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.favorites_count, favoriteSongs.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    favoriteSongs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                title = stringResource(R.string.favorites_empty_title),
                                message = stringResource(R.string.favorites_empty_message),
                                icon = Icons.Rounded.FavoriteBorder
                            )
                        }
                    }
                    layoutSpec.listColumns > 1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(layoutSpec.listColumns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = favoriteSongs,
                                key = { "favorite_${it.id}" }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, favoriteSongs) },
                                    trailingContent = {
                                        SongRowActions(
                                            songTitle = song.title,
                                            isFavorite = true,
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
                            items(
                                items = favoriteSongs,
                                key = { "favorite_${it.id}" }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, favoriteSongs) },
                                    trailingContent = {
                                        SongRowActions(
                                            songTitle = song.title,
                                            isFavorite = true,
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
