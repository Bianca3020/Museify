package com.biancaputri.museify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.biancaputri.museify.data.preferences.AppLanguage
import com.biancaputri.museify.data.preferences.SettingsDataStore
import com.biancaputri.museify.data.preferences.ThemeMode
import com.biancaputri.museify.ui.components.MuseifyBrandHeader
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
                    MuseifyFoundationSurface()
                }
            }
        }
    }
}

@Composable
private fun MuseifyFoundationSurface(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MuseifyBrandHeader()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.foundation_ready_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MuseifyFoundationLightPreview() {
    MuseifyTheme(themeMode = ThemeMode.LIGHT) {
        MuseifyFoundationSurface()
    }
}

@Preview(showBackground = true)
@Composable
private fun MuseifyFoundationDarkPreview() {
    MuseifyTheme(themeMode = ThemeMode.DARK) {
        MuseifyFoundationSurface()
    }
}
