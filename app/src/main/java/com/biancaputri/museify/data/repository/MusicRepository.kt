package com.biancaputri.museify.data.repository

import com.biancaputri.museify.BuildConfig
import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.RetrofitInstance
import com.biancaputri.museify.data.api.toSongOrNull
import com.biancaputri.museify.data.model.Song
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

sealed interface MusicError {
    data object MissingCredentials : MusicError
    data object InvalidCredentials : MusicError
    data object Network : MusicError
    data object Timeout : MusicError
    data class Api(val message: String?) : MusicError
    data object Unknown : MusicError
}

sealed interface MusicResult<out T> {
    data class Success<T>(val data: T) : MusicResult<T>
    data class Error(val error: MusicError) : MusicResult<Nothing>
}

/**
 * Coordinates music catalog queries against Jamendo and maps API/network failures cleanly.
 */
class MusicRepository(
    private val api: JamendoApi = RetrofitInstance.api,
    private val clientIdProvider: () -> String = { BuildConfig.JAMENDO_CLIENT_ID }
) {

    suspend fun getFeaturedTracks(limit: Int = 12): MusicResult<List<Song>> {
        return executeTrackQuery(limit = limit, order = "popularity_week")
    }

    suspend fun getRecommendedTracks(limit: Int = 12): MusicResult<List<Song>> {
        return executeTrackQuery(limit = limit, offset = 12, order = "popularity_month")
    }

    suspend fun getCuratedTracks(limit: Int = 20): MusicResult<List<Song>> {
        return executeTrackQuery(limit = limit, order = "releasedate_desc")
    }

    suspend fun searchTracks(query: String, limit: Int = 25): MusicResult<List<Song>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return MusicResult.Success(emptyList())
        }
        return executeTrackQuery(limit = limit, search = trimmedQuery)
    }

    private suspend fun executeTrackQuery(
        limit: Int,
        offset: Int = 0,
        search: String? = null,
        order: String? = null,
        tags: String? = null
    ): MusicResult<List<Song>> {
        val clientId = clientIdProvider().trim()
        if (clientId.isEmpty()) {
            return MusicResult.Error(MusicError.MissingCredentials)
        }

        return try {
            val response = api.getTracks(
                clientId = clientId,
                limit = limit,
                offset = offset,
                search = search,
                order = order,
                tags = tags
            )

            val headers = response.headers
            if (headers?.status.equals("failed", ignoreCase = true)) {
                val errorMessage = headers?.errorMessage?.takeIf { it.isNotBlank() }
                val isCredentialError = headers?.code == 5 ||
                    errorMessage?.contains("client_id", ignoreCase = true) == true
                return if (isCredentialError) {
                    MusicResult.Error(MusicError.InvalidCredentials)
                } else {
                    MusicResult.Error(MusicError.Api(errorMessage))
                }
            }

            val songs = response.results
                .orEmpty()
                .mapNotNull { it.toSongOrNull() }
            MusicResult.Success(songs)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SocketTimeoutException) {
            MusicResult.Error(MusicError.Timeout)
        } catch (_: UnknownHostException) {
            MusicResult.Error(MusicError.Network)
        } catch (_: IOException) {
            MusicResult.Error(MusicError.Network)
        } catch (httpException: HttpException) {
            if (httpException.code() == 401 || httpException.code() == 403) {
                MusicResult.Error(MusicError.InvalidCredentials)
            } else {
                MusicResult.Error(MusicError.Api(httpException.message()))
            }
        } catch (_: Exception) {
            MusicResult.Error(MusicError.Unknown)
        }
    }
}
