package com.biancaputri.museify

import androidx.compose.ui.graphics.toArgb
import com.biancaputri.museify.data.preferences.AppLanguage
import com.biancaputri.museify.data.preferences.ThemeMode
import com.biancaputri.museify.ui.theme.DeepPlum
import com.biancaputri.museify.ui.theme.DeepSlateBlue
import com.biancaputri.museify.ui.theme.ElectricAqua
import com.biancaputri.museify.ui.theme.IceMint
import com.biancaputri.museify.ui.theme.MidnightNavy
import com.biancaputri.museify.ui.theme.MuseifyDarkColorScheme
import com.biancaputri.museify.ui.theme.MuseifyLightColorScheme
import com.biancaputri.museify.ui.theme.MutedSteelBlue
import com.biancaputri.museify.ui.theme.OceanBlue
import com.biancaputri.museify.ui.theme.PureWhite
import com.biancaputri.museify.ui.theme.SoftMint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeAndPreferencesTest {

    @Test
    fun themeMode_fromKey_resolvesAllModesWithSystemFallback() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("unknown"))
    }

    @Test
    fun appLanguage_fromTag_resolvesSupportedTags() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromTag("in"))
        assertNull(AppLanguage.fromTag("fr"))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test
    fun darkColorScheme_matchesOfficialBrandAssignments() {
        assertEquals(MidnightNavy.toArgb(), MuseifyDarkColorScheme.background.toArgb())
        assertEquals(DeepSlateBlue.toArgb(), MuseifyDarkColorScheme.surface.toArgb())
        assertEquals(MutedSteelBlue.toArgb(), MuseifyDarkColorScheme.surfaceVariant.toArgb())
        assertEquals(DeepPlum.toArgb(), MuseifyDarkColorScheme.tertiary.toArgb())
        assertEquals(ElectricAqua.toArgb(), MuseifyDarkColorScheme.primary.toArgb())
        assertEquals(OceanBlue.toArgb(), MuseifyDarkColorScheme.secondary.toArgb())
        assertEquals(IceMint.toArgb(), MuseifyDarkColorScheme.onBackground.toArgb())
        assertEquals(SoftMint.toArgb(), MuseifyDarkColorScheme.onSurfaceVariant.toArgb())
    }

    @Test
    fun lightColorScheme_matchesOfficialBrandAssignments() {
        assertEquals(IceMint.toArgb(), MuseifyLightColorScheme.background.toArgb())
        assertEquals(PureWhite.toArgb(), MuseifyLightColorScheme.surface.toArgb())
        assertEquals(SoftMint.toArgb(), MuseifyLightColorScheme.surfaceVariant.toArgb())
        assertEquals(DeepSlateBlue.toArgb(), MuseifyLightColorScheme.primary.toArgb())
        assertEquals(OceanBlue.toArgb(), MuseifyLightColorScheme.secondary.toArgb())
        assertEquals(MidnightNavy.toArgb(), MuseifyLightColorScheme.onBackground.toArgb())
        assertEquals(MutedSteelBlue.toArgb(), MuseifyLightColorScheme.onSurfaceVariant.toArgb())
        assertEquals(DeepPlum.toArgb(), MuseifyLightColorScheme.tertiary.toArgb())
        assertEquals(ElectricAqua.toArgb(), MuseifyLightColorScheme.primaryContainer.toArgb())
    }
}
