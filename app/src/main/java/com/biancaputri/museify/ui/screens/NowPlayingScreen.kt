package com.biancaputri.museify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.biancaputri.museify.R
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.player.PlaybackError
import com.biancaputri.museify.player.PlaybackUiState
import com.biancaputri.museify.ui.components.ArtworkImage
import com.biancaputri.museify.ui.components.EmptyStateView
import com.biancaputri.museify.ui.util.computeAdaptiveLayoutSpec
import com.biancaputri.museify.ui.util.formatDurationMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenDetails: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.now_playing_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.nav_back)
                            )
                        }
                    },
                    actions = {
                        if (song != null) {
                            IconButton(onClick = { onToggleFavorite(song) }) {
                                Icon(
                                    imageVector = if (isFavorite) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = stringResource(
                                        if (isFavorite) {
                                            R.string.action_remove_favorite
                                        } else {
                                            R.string.action_add_favorite
                                        }
                                    ),
                                    tint = if (isFavorite) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                            IconButton(onClick = { onOpenDetails(song) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(
                                        R.string.cd_song_info,
                                        song.title
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            if (song == null) {
                EmptyStateView(
                    title = stringResource(R.string.player_no_track),
                    message = stringResource(R.string.home_subtitle),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                return@Scaffold
            }

            val artistDisplay = song.artistName.ifBlank {
                stringResource(R.string.song_details_unknown_artist)
            }
            val effectiveDurationMs = playbackState.effectiveDurationMs
            var isScrubbing by remember(song.id) { mutableStateOf(false) }
            var scrubFraction by remember(song.id) { mutableFloatStateOf(0f) }

            val liveFraction = if (effectiveDurationMs > 0L) {
                (playbackState.positionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val sliderValue = if (isScrubbing) scrubFraction else liveFraction
            val displayedPositionMs = if (isScrubbing && effectiveDurationMs > 0L) {
                (scrubFraction * effectiveDurationMs).toLong()
            } else {
                playbackState.positionMs
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val layoutSpec = computeAdaptiveLayoutSpec(
                    widthDp = maxWidth.value.toInt(),
                    heightDp = maxHeight.value.toInt()
                )

                if (layoutSpec.useTwoPaneNowPlaying) {
                    val artworkSide = min(maxWidth * 0.38f, maxHeight * 0.78f)
                        .coerceIn(160.dp, 320.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = layoutSpec.horizontalPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            ArtworkImage(
                                artworkUrl = song.artworkUrl,
                                songTitle = song.title,
                                modifier = Modifier.size(artworkSide),
                                cornerRadius = 22.dp,
                                fallbackIconSize = 56.dp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TrackMetadataHeader(
                                song = song,
                                artistDisplay = artistDisplay,
                                compact = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PlayerControlsSection(
                                song = song,
                                playbackState = playbackState,
                                sliderValue = sliderValue,
                                displayedPositionMs = displayedPositionMs,
                                effectiveDurationMs = effectiveDurationMs,
                                onScrubChange = { fraction ->
                                    isScrubbing = true
                                    scrubFraction = fraction
                                },
                                onScrubFinished = {
                                    if (isScrubbing && effectiveDurationMs > 0L) {
                                        onSeek((scrubFraction * effectiveDurationMs).toLong())
                                    }
                                    isScrubbing = false
                                },
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onPrevious = onPrevious,
                                onRetry = onRetry,
                                compact = true
                            )
                        }
                    }
                } else {
                    val portraitArtworkSide = min(maxWidth * 0.78f, maxHeight * 0.42f)
                        .coerceIn(172.dp, 320.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = layoutSpec.horizontalPadding, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ArtworkImage(
                                artworkUrl = song.artworkUrl,
                                songTitle = song.title,
                                modifier = Modifier.size(portraitArtworkSide),
                                cornerRadius = 24.dp,
                                fallbackIconSize = 60.dp
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            TrackMetadataHeader(
                                song = song,
                                artistDisplay = artistDisplay,
                                compact = false
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PlayerControlsSection(
                                song = song,
                                playbackState = playbackState,
                                sliderValue = sliderValue,
                                displayedPositionMs = displayedPositionMs,
                                effectiveDurationMs = effectiveDurationMs,
                                onScrubChange = { fraction ->
                                    isScrubbing = true
                                    scrubFraction = fraction
                                },
                                onScrubFinished = {
                                    if (isScrubbing && effectiveDurationMs > 0L) {
                                        onSeek((scrubFraction * effectiveDurationMs).toLong())
                                    }
                                    isScrubbing = false
                                },
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onPrevious = onPrevious,
                                onRetry = onRetry,
                                compact = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackMetadataHeader(
    song: Song,
    artistDisplay: String,
    compact: Boolean
) {
    Text(
        text = song.title,
        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = artistDisplay,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    if (!song.albumName.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.albumName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayerControlsSection(
    song: Song,
    playbackState: PlaybackUiState,
    sliderValue: Float,
    displayedPositionMs: Long,
    effectiveDurationMs: Long,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRetry: () -> Unit,
    compact: Boolean
) {
    if (playbackState.error != null) {
        PlaybackErrorBanner(
            error = playbackState.error,
            onRetry = onRetry
        )
        Spacer(modifier = Modifier.height(12.dp))
    } else if (playbackState.isBuffering) {
        Text(
            text = stringResource(R.string.player_buffering),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    Slider(
        value = sliderValue,
        onValueChange = { fraction ->
            if (effectiveDurationMs > 0L && song.hasPlayableAudio) {
                onScrubChange(fraction)
            }
        },
        onValueChangeFinished = onScrubFinished,
        enabled = effectiveDurationMs > 0L && song.hasPlayableAudio,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDurationMillis(displayedPositionMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatDurationMillis(effectiveDurationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(if (compact) 14.dp else 20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val canGoPrevious = playbackState.hasPrevious || displayedPositionMs > 3000L
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                modifier = Modifier.size(34.dp),
                tint = if (canGoPrevious) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        FilledIconButton(
            onClick = onPlayPause,
            enabled = song.hasPlayableAudio,
            modifier = Modifier.size(if (compact) 62.dp else 70.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (playbackState.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (playbackState.isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (playbackState.isPlaying) R.string.player_pause else R.string.player_play
                    ),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(
            onClick = onNext,
            enabled = playbackState.hasNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                modifier = Modifier.size(34.dp),
                tint = if (playbackState.hasNext) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }
    }
}

@Composable
private fun PlaybackErrorBanner(
    error: PlaybackError,
    onRetry: () -> Unit
) {
    val messageRes = when (error) {
        PlaybackError.UNAVAILABLE_AUDIO -> R.string.player_unavailable_audio
        PlaybackError.PLAYBACK_FAILED -> R.string.player_playback_error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (error == PlaybackError.PLAYBACK_FAILED) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.action_retry),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
