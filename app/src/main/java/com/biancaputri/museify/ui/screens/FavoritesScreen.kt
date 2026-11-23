package com.biancaputri.museify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.R
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.ui.components.EmptyStateView
import com.biancaputri.museify.ui.components.SongListItem
import com.biancaputri.museify.ui.components.SongRowActions

@Composable
fun FavoritesScreen(
    favoriteSongs: List<Song>,
    activeSongId: String?,
    onSongPlay: (Song, List<Song>) -> Unit,
    onSongDetails: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
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
            if (favoriteSongs.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.favorites_empty_title),
                    message = stringResource(R.string.favorites_empty_message),
                    icon = Icons.Rounded.FavoriteBorder,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
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
