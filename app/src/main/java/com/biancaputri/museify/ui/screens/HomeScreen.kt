package com.biancaputri.museify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.R
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.ui.components.CompactSongCard
import com.biancaputri.museify.ui.components.EmptyStateView
import com.biancaputri.museify.ui.components.ErrorStateView
import com.biancaputri.museify.ui.components.FeaturedSongCard
import com.biancaputri.museify.ui.components.LoadingState
import com.biancaputri.museify.ui.components.MuseifyBrandHeader
import com.biancaputri.museify.ui.components.MuseifySearchTriggerBar
import com.biancaputri.museify.ui.components.SongListItem
import com.biancaputri.museify.ui.util.greetingResForHour
import com.biancaputri.museify.ui.util.toStringRes
import com.biancaputri.museify.viewmodel.HomeUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    homeState: HomeUiState,
    recentSongs: List<Song>,
    activeSongId: String?,
    onSearchClick: () -> Unit,
    onSongPlay: (Song, List<Song>) -> Unit,
    onSongDetails: (Song) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "home_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                MuseifyBrandHeader()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(greetingResForHour(currentHour)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                MuseifySearchTriggerBar(onClick = onSearchClick)
            }
        }

        when {
            homeState.isLoading -> {
                item(key = "home_loading") {
                    LoadingState(modifier = Modifier.padding(top = 32.dp))
                }
            }
            homeState.error != null -> {
                item(key = "home_error") {
                    ErrorStateView(
                        message = stringResource(homeState.error.toStringRes()),
                        onRetry = onRetry,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            homeState.isEmpty -> {
                item(key = "home_empty") {
                    EmptyStateView(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = onRetry,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            else -> {
                if (homeState.featuredSongs.isNotEmpty()) {
                    item(key = "featured_section") {
                        SectionHeader(
                            title = stringResource(R.string.section_featured),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = homeState.featuredSongs,
                                key = { "featured_${it.id}" }
                            ) { song ->
                                FeaturedSongCard(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, homeState.featuredSongs) }
                                )
                            }
                        }
                    }
                }

                if (recentSongs.isNotEmpty()) {
                    item(key = "recent_section") {
                        SectionHeader(
                            title = stringResource(R.string.section_recently_played),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = recentSongs,
                                key = { "recent_${it.id}" }
                            ) { song ->
                                CompactSongCard(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, recentSongs) }
                                )
                            }
                        }
                    }
                }

                if (homeState.recommendedSongs.isNotEmpty()) {
                    item(key = "recommended_section") {
                        SectionHeader(
                            title = stringResource(R.string.section_recommended),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = homeState.recommendedSongs,
                                key = { "recommended_${it.id}" }
                            ) { song ->
                                CompactSongCard(
                                    song = song,
                                    isActive = song.id == activeSongId,
                                    onClick = { onSongPlay(song, homeState.recommendedSongs) }
                                )
                            }
                        }
                    }
                }

                if (homeState.curatedSongs.isNotEmpty()) {
                    item(key = "curated_header") {
                        SectionHeader(
                            title = stringResource(R.string.section_curated),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    items(
                        items = homeState.curatedSongs,
                        key = { "curated_${it.id}" }
                    ) { song ->
                        SongListItem(
                            song = song,
                            isActive = song.id == activeSongId,
                            onClick = { onSongPlay(song, homeState.curatedSongs) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                            trailingContent = {
                                IconButton(onClick = { onSongDetails(song) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = stringResource(
                                            R.string.cd_song_info,
                                            song.title
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}
