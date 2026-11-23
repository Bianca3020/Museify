package com.biancaputri.museify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.biancaputri.museify.data.model.Song

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumName: String?,
    val artworkUrl: String?,
    val audioUrl: String?,
    val durationSeconds: Int,
    val shareUrl: String?,
    val licenseUrl: String?,
    val licenseSummary: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artistName = artistName,
            albumName = albumName,
            artworkUrl = artworkUrl,
            audioUrl = audioUrl,
            durationSeconds = durationSeconds,
            shareUrl = shareUrl,
            licenseUrl = licenseUrl,
            licenseSummary = licenseSummary
        )
    }
}

fun Song.toFavoriteEntity(addedAt: Long = System.currentTimeMillis()): FavoriteEntity {
    return FavoriteEntity(
        id = id,
        title = title,
        artistName = artistName,
        albumName = albumName,
        artworkUrl = artworkUrl,
        audioUrl = audioUrl,
        durationSeconds = durationSeconds,
        shareUrl = shareUrl,
        licenseUrl = licenseUrl,
        licenseSummary = licenseSummary,
        addedAt = addedAt
    )
}
