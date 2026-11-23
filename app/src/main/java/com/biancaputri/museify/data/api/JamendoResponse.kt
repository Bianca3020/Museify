package com.biancaputri.museify.data.api

import com.biancaputri.museify.data.model.Song
import com.google.gson.annotations.SerializedName

data class JamendoTracksResponse(
    @SerializedName("headers") val headers: JamendoHeadersDto? = null,
    @SerializedName("results") val results: List<JamendoTrackDto>? = null
)

data class JamendoHeadersDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: Int? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("warnings") val warnings: String? = null,
    @SerializedName("results_count") val resultsCount: Int? = null
)

data class JamendoTrackDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("artist_name") val artistName: String? = null,
    @SerializedName("album_name") val albumName: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("album_image") val albumImage: String? = null,
    @SerializedName("audio") val audio: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("shareurl") val shareUrl: String? = null,
    @SerializedName("shorturl") val shortUrl: String? = null,
    @SerializedName("license_ccurl") val licenseCcUrl: String? = null,
    @SerializedName("licenses") val licenses: JamendoLicensesDto? = null
)

data class JamendoLicensesDto(
    @SerializedName("ccnc") val ccnc: Boolean? = null,
    @SerializedName("ccnd") val ccnd: Boolean? = null,
    @SerializedName("ccsa") val ccsa: Boolean? = null,
    @SerializedName("cczero") val cczero: Boolean? = null,
    @SerializedName("pro") val pro: Boolean? = null
)

/**
 * Maps a Jamendo track DTO into a domain [Song], returning null when required identity fields are missing.
 * Uses only the `audio` streaming field and never `audiodownload`.
 */
fun JamendoTrackDto.toSongOrNull(): Song? {
    val cleanId = id?.trim().orEmpty()
    val cleanTitle = name?.trim().orEmpty()
    if (cleanId.isEmpty() || cleanTitle.isEmpty()) return null

    val cleanArtwork = image?.trim()?.takeIf { it.isNotEmpty() }
        ?: albumImage?.trim()?.takeIf { it.isNotEmpty() }

    val cleanLicenseUrl = licenseCcUrl?.trim()?.takeIf { it.isNotEmpty() }

    return Song(
        id = cleanId,
        title = cleanTitle,
        artistName = artistName?.trim().orEmpty(),
        albumName = albumName?.trim()?.takeIf { it.isNotEmpty() },
        artworkUrl = cleanArtwork,
        audioUrl = audio?.trim()?.takeIf { it.isNotEmpty() },
        durationSeconds = (duration ?: 0).coerceAtLeast(0),
        shareUrl = shareUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: shortUrl?.trim()?.takeIf { it.isNotEmpty() },
        licenseUrl = cleanLicenseUrl,
        licenseSummary = buildLicenseSummary(cleanLicenseUrl, licenses)
    )
}

internal fun buildLicenseSummary(
    licenseUrl: String?,
    licenses: JamendoLicensesDto?
): String? {
    if (!licenseUrl.isNullOrBlank()) {
        val ccMatch = Regex("creativecommons\\.org/(?:licenses|publicdomain)/([^/]+)/([0-9.]+)?", RegexOption.IGNORE_CASE)
            .find(licenseUrl)
        if (ccMatch != null) {
            val code = ccMatch.groupValues[1].uppercase()
            val version = ccMatch.groupValues[2].takeIf { it.isNotBlank() }
            val prefix = if (code == "ZERO") "CC0" else "CC $code"
            return if (version != null) "$prefix $version" else prefix
        }
    }

    if (licenses != null) {
        if (licenses.cczero == true) return "CC0"
        val parts = buildList {
            add("BY")
            if (licenses.ccnc == true) add("NC")
            if (licenses.ccnd == true) add("ND")
            if (licenses.ccsa == true) add("SA")
        }
        return "CC ${parts.joinToString("-")}"
    }

    return null
}
