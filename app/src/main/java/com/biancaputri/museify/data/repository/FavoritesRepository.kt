package com.biancaputri.museify.data.repository

import com.biancaputri.museify.data.local.FavoriteDao
import com.biancaputri.museify.data.local.toFavoriteEntity
import com.biancaputri.museify.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoritesStore {
    fun observeFavorites(): Flow<List<Song>>
    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun toggleFavorite(song: Song): Result<Boolean>
}

/**
 * Room-backed implementation of [FavoritesStore] for persistent saved songs.
 */
class FavoritesRepository(
    private val favoriteDao: FavoriteDao
) : FavoritesStore {

    override fun observeFavorites(): Flow<List<Song>> {
        return favoriteDao.observeFavorites().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun observeFavoriteIds(): Flow<Set<String>> {
        return favoriteDao.observeFavoriteIds().map { ids -> ids.toSet() }
    }

    override suspend fun toggleFavorite(song: Song): Result<Boolean> {
        return runCatching {
            val currentlyFavorite = favoriteDao.isFavorite(song.id)
            if (currentlyFavorite) {
                favoriteDao.deleteFavoriteById(song.id)
                false
            } else {
                favoriteDao.upsertFavorite(song.toFavoriteEntity())
                true
            }
        }
    }
}
