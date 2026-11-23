package com.biancaputri.museify.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.biancaputri.museify.data.preferences.ThemeMode

val MuseifyDarkColorScheme = darkColorScheme(
    primary = ElectricAqua,
    onPrimary = MidnightNavy,
    primaryContainer = DeepSlateBlue,
    onPrimaryContainer = IceMint,
    secondary = OceanBlue,
    onSecondary = MidnightNavy,
    secondaryContainer = MutedSteelBlue,
    onSecondaryContainer = IceMint,
    tertiary = DeepPlum,
    onTertiary = IceMint,
    tertiaryContainer = DeepPlum,
    onTertiaryContainer = SoftMint,
    background = MidnightNavy,
    onBackground = IceMint,
    surface = DeepSlateBlue,
    onSurface = IceMint,
    surfaceVariant = MutedSteelBlue,
    onSurfaceVariant = SoftMint,
    surfaceContainerLowest = MidnightNavy,
    surfaceContainerLow = DeepSlateBlue,
    surfaceContainer = DeepSlateBlue,
    surfaceContainerHigh = MutedSteelBlue,
    surfaceContainerHighest = MutedSteelBlue,
    outline = OceanBlue,
    outlineVariant = MutedSteelBlue,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

val MuseifyLightColorScheme = lightColorScheme(
    primary = DeepSlateBlue,
    onPrimary = PureWhite,
    primaryContainer = ElectricAqua,
    onPrimaryContainer = MidnightNavy,
    secondary = OceanBlue,
    onSecondary = PureWhite,
    secondaryContainer = SoftMint,
    onSecondaryContainer = MidnightNavy,
    tertiary = DeepPlum,
    onTertiary = PureWhite,
    tertiaryContainer = DeepPlum,
    onTertiaryContainer = IceMint,
    background = IceMint,
    onBackground = MidnightNavy,
    surface = PureWhite,
    onSurface = MidnightNavy,
    surfaceVariant = SoftMint,
    onSurfaceVariant = MutedSteelBlue,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = SoftMint,
    surfaceContainerHigh = SoftMint,
    surfaceContainerHighest = PureWhite,
    outline = OceanBlue,
    outlineVariant = SoftMint,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

@Composable
private fun animateThemeColor(target: Color): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 280),
        label = "MuseifyThemeColor"
    )
    return animated
}

@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateThemeColor(target.primary),
        onPrimary = animateThemeColor(target.onPrimary),
        primaryContainer = animateThemeColor(target.primaryContainer),
        onPrimaryContainer = animateThemeColor(target.onPrimaryContainer),
        secondary = animateThemeColor(target.secondary),
        onSecondary = animateThemeColor(target.onSecondary),
        secondaryContainer = animateThemeColor(target.secondaryContainer),
        onSecondaryContainer = animateThemeColor(target.onSecondaryContainer),
        tertiary = animateThemeColor(target.tertiary),
        onTertiary = animateThemeColor(target.onTertiary),
        tertiaryContainer = animateThemeColor(target.tertiaryContainer),
        onTertiaryContainer = animateThemeColor(target.onTertiaryContainer),
        background = animateThemeColor(target.background),
        onBackground = animateThemeColor(target.onBackground),
        surface = animateThemeColor(target.surface),
        onSurface = animateThemeColor(target.onSurface),
        surfaceVariant = animateThemeColor(target.surfaceVariant),
        onSurfaceVariant = animateThemeColor(target.onSurfaceVariant),
        surfaceContainerLowest = animateThemeColor(target.surfaceContainerLowest),
        surfaceContainerLow = animateThemeColor(target.surfaceContainerLow),
        surfaceContainer = animateThemeColor(target.surfaceContainer),
        surfaceContainerHigh = animateThemeColor(target.surfaceContainerHigh),
        surfaceContainerHighest = animateThemeColor(target.surfaceContainerHighest),
        outline = animateThemeColor(target.outline),
        outlineVariant = animateThemeColor(target.outlineVariant)
    )
}

/**
 * Resolves whether dark mode is active for the given [ThemeMode].
 */
@Composable
fun isMuseifyDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

/**
 * Applies Museify's adaptive Light or Dark Material 3 theme without dynamic color overrides.
 */
@Composable
fun MuseifyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = isMuseifyDarkTheme(themeMode)
    val targetScheme = if (useDarkTheme) MuseifyDarkColorScheme else MuseifyLightColorScheme
    val colorScheme = animatedColorScheme(targetScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MuseifyTypography,
        content = content
    )
}
