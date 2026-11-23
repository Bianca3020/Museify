package com.biancaputri.museify.ui.util

import androidx.annotation.StringRes
import com.biancaputri.museify.R
import com.biancaputri.museify.data.repository.MusicError
import java.util.Locale

/**
 * Formats a duration in seconds into `m:ss` (or `--:--` if unavailable).
 */
fun formatDuration(durationSeconds: Int): String {
    if (durationSeconds <= 0) return "--:--"
    val totalSeconds = durationSeconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Formats a playback timestamp in milliseconds into `m:ss`.
 */
fun formatDurationMillis(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = (durationMs / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Resolves a localized greeting string resource for the given 24-hour clock hour.
 * - 05:00 - 11:59: Pagi (Morning)
 * - 12:00 - 14:59: Siang (Afternoon)
 * - 15:00 - 18:59: Sore (Late Afternoon / Evening)
 * - 19:00 - 04:59: Malam (Night / Evening)
 */
@StringRes
fun greetingResForHour(hourOfDay: Int): Int {
    return when (hourOfDay) {
        in 5..11 -> R.string.greeting_morning
        in 12..14 -> R.string.greeting_afternoon
        in 15..18 -> R.string.greeting_late_afternoon
        else -> R.string.greeting_evening
    }
}

/**
 * Maps a domain [MusicError] into a localized user-friendly string resource ID.
 */
@StringRes
fun MusicError.toStringRes(): Int {
    return when (this) {
        MusicError.MissingCredentials,
        MusicError.InvalidCredentials -> R.string.error_api_credentials
        MusicError.Network -> R.string.error_network
        MusicError.Timeout -> R.string.error_timeout
        is MusicError.Api,
        MusicError.Unknown -> R.string.error_generic
    }
}
