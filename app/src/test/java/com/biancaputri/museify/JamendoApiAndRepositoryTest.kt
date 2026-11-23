package com.biancaputri.museify

import com.biancaputri.museify.data.api.JamendoApi
import com.biancaputri.museify.data.api.JamendoHeadersDto
import com.biancaputri.museify.data.api.JamendoTracksResponse
import com.biancaputri.museify.data.repository.MusicError
import com.biancaputri.museify.data.repository.MusicRepository
import com.biancaputri.museify.data.repository.MusicResult
import com.google.gson.Gson
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamendoApiAndRepositoryTest {

    private val gson = Gson()

    @Test
    fun parsesJamendoTracksResponse_andUsesAudioInsteadOfAudiodownload() = runBlocking {
        val sampleJson = """
            {
              "headers": {
                "status": "success",
                "code": 0,
                "error_message": "",
                "results_count": 2
              },
              "results": [
                {
                  "id": "1884561",
                  "name": "Midnight Horizon",
                  "duration": 204,
                  "artist_name": "Nova Echo",
                  "album_name": "Electric Tides",
                  "license_ccurl": "https://creativecommons.org/licenses/by-nc-sa/3.0/",
                  "album_image": "https://img.example/album.jpg",
                  "image": "https://img.example/track300.jpg",
                  "audio": "https://stream.example/mp32/1884561",
                  "audiodownload": "https://download.example/1884561",
                  "shareurl": "https://www.jamendo.com/track/1884561",
                  "licenses": {
                    "ccnc": true,
                    "ccnd": false,
                    "ccsa": true,
                    "cczero": false,
                    "pro": false
                  }
                },
                {
                  "id": "1884562",
                  "name": "Silent Drift",
                  "duration": null,
                  "artist_name": null,
                  "album_name": "",
                  "album_image": "https://img.example/fallback-album.jpg",
                  "image": "",
                  "audio": "",
                  "audiodownload": "https://download.example/should-not-be-used",
                  "shorturl": "https://jamen.do/t/1884562",
                  "licenses": {
                    "ccnc": false,
                    "ccnd": false,
                    "ccsa": false,
                    "cczero": true,
                    "pro": false
                  }
                },
                {
                  "id": "",
                  "name": "Invalid Missing Id"
                }
              ]
            }
        """.trimIndent()

        val parsed = gson.fromJson(sampleJson, JamendoTracksResponse::class.java)
        var capturedSearch: String? = null
        val fakeApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse {
                capturedSearch = search
                assertEquals("test-client-id", clientId)
                assertEquals("json", format)
                assertEquals("licenses", include)
                assertEquals("mp32", audioFormat)
                assertEquals(300, imageSize)
                return parsed
            }
        }

        val repository = MusicRepository(api = fakeApi, clientIdProvider = { "test-client-id" })
        val result = repository.searchTracks("  horizon  ")

        assertEquals("horizon", capturedSearch)
        assertTrue(result is MusicResult.Success)
        val songs = (result as MusicResult.Success).data
        assertEquals(2, songs.size)

        val first = songs[0]
        assertEquals("1884561", first.id)
        assertEquals("Midnight Horizon", first.title)
        assertEquals("Nova Echo", first.artistName)
        assertEquals("Electric Tides", first.albumName)
        assertEquals("https://img.example/track300.jpg", first.artworkUrl)
        assertEquals("https://stream.example/mp32/1884561", first.audioUrl)
        assertTrue(first.hasPlayableAudio)
        assertEquals(204, first.durationSeconds)
        assertEquals("https://www.jamendo.com/track/1884561", first.shareUrl)
        assertEquals("CC BY-NC-SA 3.0", first.licenseSummary)

        val second = songs[1]
        assertEquals("1884562", second.id)
        assertEquals("", second.artistName)
        assertNull(second.albumName)
        assertEquals("https://img.example/fallback-album.jpg", second.artworkUrl)
        assertNull(second.audioUrl)
        assertFalse(second.hasPlayableAudio)
        assertEquals(0, second.durationSeconds)
        assertEquals("https://jamen.do/t/1884562", second.shareUrl)
        assertEquals("CC0", second.licenseSummary)
    }

    @Test
    fun missingClientId_returnsMissingCredentialsErrorWithoutCallingApi() = runBlocking {
        var apiCalled = false
        val fakeApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse {
                apiCalled = true
                return JamendoTracksResponse()
            }
        }

        val repository = MusicRepository(api = fakeApi, clientIdProvider = { "   " })
        val result = repository.getFeaturedTracks()

        assertFalse(apiCalled)
        assertEquals(MusicResult.Error(MusicError.MissingCredentials), result)
    }

    @Test
    fun failedJamendoHeadersAndNetworkExceptions_mapToExpectedErrors() = runBlocking {
        val failedCredentialApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse {
                return JamendoTracksResponse(
                    headers = JamendoHeadersDto(
                        status = "failed",
                        code = 5,
                        errorMessage = "invalid client_id"
                    ),
                    results = emptyList()
                )
            }
        }

        val timeoutApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse {
                throw SocketTimeoutException("timeout")
            }
        }

        val offlineApi = object : JamendoApi {
            override suspend fun getTracks(
                clientId: String,
                format: String,
                limit: Int,
                offset: Int,
                search: String?,
                order: String?,
                tags: String?,
                include: String,
                audioFormat: String,
                imageSize: Int
            ): JamendoTracksResponse {
                throw UnknownHostException("api.jamendo.com")
            }
        }

        assertEquals(
            MusicResult.Error(MusicError.InvalidCredentials),
            MusicRepository(failedCredentialApi) { "abc" }.getCuratedTracks()
        )
        assertEquals(
            MusicResult.Error(MusicError.Timeout),
            MusicRepository(timeoutApi) { "abc" }.getRecommendedTracks()
        )
        assertEquals(
            MusicResult.Error(MusicError.Network),
            MusicRepository(offlineApi) { "abc" }.getFeaturedTracks()
        )
        assertEquals(
            MusicResult.Success(emptyList<com.biancaputri.museify.data.model.Song>()),
            MusicRepository(offlineApi) { "abc" }.searchTracks("   ")
        )
    }
}
