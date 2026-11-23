package com.biancaputri.museify.data.model

/**
 * Domain representation of a music track returned by Jamendo.
 */
data class Song(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val audioUrl: String?,
    val durationSeconds: Int,
    val shareUrl: String?,
    val licenseUrl: String?,
    val licenseSummary: String?
) {
    val hasPlayableAudio: Boolean
        get() = !audioUrl.isNullOrBlank()
}
