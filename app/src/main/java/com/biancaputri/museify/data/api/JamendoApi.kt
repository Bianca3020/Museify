package com.biancaputri.museify.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service definition for the Jamendo v3.0 REST API.
 */
interface JamendoApi {

    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("order") order: String? = null,
        @Query("tags") tags: String? = null,
        @Query("include") include: String = "licenses",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 300
    ): JamendoTracksResponse
}
