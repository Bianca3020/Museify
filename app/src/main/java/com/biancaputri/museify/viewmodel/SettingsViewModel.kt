package com.biancaputri.museify.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.biancaputri.museify.data.preferences.AppLanguage
import com.biancaputri.museify.data.preferences.SettingsDataStore
import com.biancaputri.museify.data.preferences.SettingsStore
import com.biancaputri.museify.data.preferences.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes persisted theme and language preferences and commits user updates to DataStore.
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeModeFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM
        )

    val appLanguage: StateFlow<AppLanguage> = settingsStore.languageFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppLanguage.deviceDefault()
        )

    fun setThemeMode(mode: ThemeMode) {
        scope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        scope.launch {
            settingsStore.setLanguage(language)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(SettingsDataStore(appContext)) as T
                }
            }
        }
    }
}
