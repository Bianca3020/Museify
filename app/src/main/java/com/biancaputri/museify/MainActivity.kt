package com.biancaputri.museify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.biancaputri.museify.data.preferences.AppLanguage
import com.biancaputri.museify.data.preferences.SettingsDataStore
import com.biancaputri.museify.data.preferences.ThemeMode
import com.biancaputri.museify.navigation.MuseifyNavHost
import com.biancaputri.museify.ui.theme.MuseifyTheme
import com.biancaputri.museify.ui.theme.ProvideAppLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsDataStore = remember { SettingsDataStore(applicationContext) }
            val themeMode by settingsDataStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val appLanguage by settingsDataStore.languageFlow.collectAsState(
                initial = AppLanguage.deviceDefault()
            )

            MuseifyTheme(themeMode = themeMode) {
                ProvideAppLanguage(language = appLanguage) {
                    MuseifyNavHost()
                }
            }
        }
    }
}
