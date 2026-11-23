package com.biancaputri.museify.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "museify_settings"
)

/**
 * Supported appearance modes for Museify.
 */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: SYSTEM
        }
    }
}

/**
 * Supported application UI languages.
 * Resource qualifier "in" corresponds to Bahasa Indonesia across Android API levels.
 */
enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    INDONESIAN("in");

    fun toLocale(): Locale = Locale.forLanguageTag(languageTag)

    companion object {
        fun fromTag(tag: String?): AppLanguage? {
            return entries.firstOrNull { it.languageTag == tag }
        }

        fun deviceDefault(): AppLanguage {
            val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
            return if (deviceLanguage == "in" || deviceLanguage == "id") {
                INDONESIAN
            } else {
                ENGLISH
            }
        }
    }
}

/**
 * Contract for reading and persisting Museify appearance and language preferences.
 */
interface SettingsStore {
    val themeModeFlow: Flow<ThemeMode>
    val languageFlow: Flow<AppLanguage>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
}

/**
 * Persists and exposes user appearance and language preferences using Jetpack DataStore.
 */
class SettingsDataStore(private val context: Context) : SettingsStore {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    private val safePreferencesFlow: Flow<Preferences> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val themeModeFlow: Flow<ThemeMode> = safePreferencesFlow.map { preferences ->
        ThemeMode.fromKey(preferences[Keys.THEME_MODE])
    }

    override val languageFlow: Flow<AppLanguage> = safePreferencesFlow.map { preferences ->
        AppLanguage.fromTag(preferences[Keys.APP_LANGUAGE]) ?: AppLanguage.deviceDefault()
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.storageKey
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.APP_LANGUAGE] = language.languageTag
        }
    }
}
