package com.biancaputri.museify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biancaputri.museify.navigation.MuseifyNavHost
import com.biancaputri.museify.ui.theme.MuseifyTheme
import com.biancaputri.museify.ui.theme.ProvideAppLanguage
import com.biancaputri.museify.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(applicationContext)
            )
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appLanguage by settingsViewModel.appLanguage.collectAsState()

            MuseifyTheme(themeMode = themeMode) {
                ProvideAppLanguage(language = appLanguage) {
                    MuseifyNavHost(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}
