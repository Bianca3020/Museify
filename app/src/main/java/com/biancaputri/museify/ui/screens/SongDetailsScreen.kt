package com.biancaputri.museify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.R
import com.biancaputri.museify.data.model.Song
import com.biancaputri.museify.ui.components.ArtworkImage
import com.biancaputri.museify.ui.components.EmptyStateView
import com.biancaputri.museify.ui.util.computeAdaptiveLayoutSpec
import com.biancaputri.museify.ui.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsScreen(
    song: Song?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onPlayClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.song_details_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (song == null) {
            EmptyStateView(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.error_generic),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        val artistDisplay = song.artistName.ifBlank {
            stringResource(R.string.song_details_unknown_artist)
        }
        val albumDisplay = song.albumName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.song_details_unknown_album)
        val licenseDisplay = song.licenseSummary?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.song_details_unknown_license)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val layoutSpec = computeAdaptiveLayoutSpec(
                widthDp = maxWidth.value.toInt(),
                heightDp = maxHeight.value.toInt()
            )

            if (layoutSpec.useTwoPaneDetails) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = layoutSpec.horizontalPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DetailsHeroBlock(
                            song = song,
                            artistDisplay = artistDisplay,
                            isFavorite = isFavorite,
                            stackButtons = false,
                            onPlayClick = onPlayClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DetailsMetadataAndLinks(
                            song = song,
                            artistDisplay = artistDisplay,
                            albumDisplay = albumDisplay,
                            licenseDisplay = licenseDisplay,
                            stackButtons = false,
                            onOpenUri = { uri -> runCatching { uriHandler.openUri(uri) } }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = layoutSpec.horizontalPadding, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DetailsHeroBlock(
                        song = song,
                        artistDisplay = artistDisplay,
                        isFavorite = isFavorite,
                        stackButtons = layoutSpec.isSmallPhone,
                        onPlayClick = onPlayClick,
                        onToggleFavorite = onToggleFavorite
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    DetailsMetadataAndLinks(
                        song = song,
                        artistDisplay = artistDisplay,
                        albumDisplay = albumDisplay,
                        licenseDisplay = licenseDisplay,
                        stackButtons = layoutSpec.isSmallPhone,
                        onOpenUri = { uri -> runCatching { uriHandler.openUri(uri) } }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailsHeroBlock(
    song: Song,
    artistDisplay: String,
    isFavorite: Boolean,
    stackButtons: Boolean,
    onPlayClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    ArtworkImage(
        artworkUrl = song.artworkUrl,
        songTitle = song.title,
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .widthIn(max = 280.dp),
        cornerRadius = 20.dp,
        fallbackIconSize = 56.dp
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = song.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = artistDisplay,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(18.dp))

    if (stackButtons) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayActionButton(
                song = song,
                onPlayClick = onPlayClick,
                modifier = Modifier.fillMaxWidth()
            )
            FavoriteActionButton(
                isFavorite = isFavorite,
                onClick = { onToggleFavorite(song) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayActionButton(
                song = song,
                onPlayClick = onPlayClick,
                modifier = Modifier.weight(1f)
            )
            FavoriteActionButton(
                isFavorite = isFavorite,
                onClick = { onToggleFavorite(song) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (!song.hasPlayableAudio) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.player_unavailable_audio),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlayActionButton(
    song: Song,
    onPlayClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onPlayClick(song) },
        enabled = song.hasPlayableAudio,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_play_now),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FavoriteActionButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(
            1.dp,
            if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Icon(
            imageVector = if (isFavorite) {
                Icons.Rounded.Favorite
            } else {
                Icons.Rounded.FavoriteBorder
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailsMetadataAndLinks(
    song: Song,
    artistDisplay: String,
    albumDisplay: String,
    licenseDisplay: String,
    stackButtons: Boolean,
    onOpenUri: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(
                label = stringResource(R.string.song_details_album),
                value = albumDisplay
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailRow(
                label = stringResource(R.string.song_details_duration),
                value = formatDuration(song.durationSeconds)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailRow(
                label = stringResource(R.string.song_details_source),
                value = stringResource(R.string.song_details_source_value)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DetailRow(
                label = stringResource(R.string.song_details_license),
                value = licenseDisplay
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.song_details_attribution),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.song_details_attribution_text,
                        artistDisplay
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    if (!song.shareUrl.isNullOrBlank() || !song.licenseUrl.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(18.dp))
        if (stackButtons) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExternalLinksContent(song = song, onOpenUri = onOpenUri, itemModifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExternalLinksContent(song = song, onOpenUri = onOpenUri, itemModifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExternalLinksContent(
    song: Song,
    onOpenUri: (String) -> Unit,
    itemModifier: Modifier
) {
    if (!song.shareUrl.isNullOrBlank()) {
        Button(
            onClick = { onOpenUri(song.shareUrl) },
            modifier = itemModifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_open_jamendo),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (!song.licenseUrl.isNullOrBlank()) {
        OutlinedButton(
            onClick = { onOpenUri(song.licenseUrl) },
            modifier = itemModifier,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = stringResource(R.string.action_view_license),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
