package com.biancaputri.museify.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.biancaputri.museify.data.preferences.AppLanguage
import java.util.Locale

/**
 * Provides a localized [Context] and [Configuration] for the Compose hierarchy so
 * `stringResource(...)` updates immediately when [language] changes without recreating the Activity.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val localizedConfig = remember(baseConfiguration, language) {
        Configuration(baseConfiguration).apply {
            setLocale(language.toLocale())
        }
    }

    val localizedContext = remember(baseContext, localizedConfig) {
        baseContext.createConfigurationContext(localizedConfig)
    }

    Locale.setDefault(language.toLocale())

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        content = content
    )
}
