package com.biancaputri.museify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.biancaputri.museify.R

/**
 * Displays album artwork with aspect-ratio preservation and a clean fallback placeholder
 * without applying theme tint filters to real artwork.
 */
@Composable
fun ArtworkImage(
    artworkUrl: String?,
    songTitle: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    fallbackIconSize: Dp = 28.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    val contentDesc = stringResource(R.string.cd_album_artwork, songTitle)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUrl.isNullOrBlank()) {
            ArtworkFallback(iconSize = fallbackIconSize)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDesc,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ArtworkFallback(iconSize = fallbackIconSize) },
                error = { ArtworkFallback(iconSize = fallbackIconSize) }
            )
        }
    }
}

@Composable
private fun ArtworkFallback(iconSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}
