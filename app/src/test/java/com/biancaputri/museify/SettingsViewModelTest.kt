package com.biancaputri.museify

import com.biancaputri.museify.data.preferences.AppLanguage
import com.biancaputri.museify.data.preferences.SettingsStore
import com.biancaputri.museify.data.preferences.ThemeMode
import com.biancaputri.museify.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun settingsViewModel_updatesAndPersistsThemeAndLanguage() {
        val fakeStore = FakeSettingsStore(
            initialTheme = ThemeMode.DARK,
            initialLanguage = AppLanguage.INDONESIAN
        )
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = SettingsViewModel(fakeStore, externalScope = testScope)

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertEquals(AppLanguage.INDONESIAN, viewModel.appLanguage.value)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setLanguage(AppLanguage.ENGLISH)

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertEquals(AppLanguage.ENGLISH, viewModel.appLanguage.value)

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        testScope.cancel()
    }

    private class FakeSettingsStore(
        initialTheme: ThemeMode,
        initialLanguage: AppLanguage
    ) : SettingsStore {
        private val themeFlow = MutableStateFlow(initialTheme)
        private val langFlow = MutableStateFlow(initialLanguage)

        override val themeModeFlow: Flow<ThemeMode> = themeFlow
        override val languageFlow: Flow<AppLanguage> = langFlow

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            themeFlow.value = themeMode
        }

        override suspend fun setLanguage(language: AppLanguage) {
            langFlow.value = language
        }
    }
}
