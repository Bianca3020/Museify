package com.biancaputri.museify

import com.biancaputri.museify.ui.util.MuseifyWidthClass
import com.biancaputri.museify.ui.util.classifyWidthClass
import com.biancaputri.museify.ui.util.computeAdaptiveLayoutSpec
import com.biancaputri.museify.ui.util.shouldUseTwoPanePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun smallPhone_usesCompactSingleColumnAndSmallerCards() {
        val spec = computeAdaptiveLayoutSpec(widthDp = 320, heightDp = 568)
        assertEquals(MuseifyWidthClass.COMPACT, spec.widthClass)
        assertTrue(spec.isSmallPhone)
        assertFalse(spec.isLandscape)
        assertFalse(spec.useNavigationRail)
        assertFalse(spec.useTwoPaneNowPlaying)
        assertEquals(1, spec.listColumns)
    }

    @Test
    fun standardPhonePortrait_usesBottomBarAndSingleColumn() {
        val spec = computeAdaptiveLayoutSpec(widthDp = 390, heightDp = 844)
        assertEquals(MuseifyWidthClass.COMPACT, spec.widthClass)
        assertFalse(spec.isSmallPhone)
        assertFalse(spec.useNavigationRail)
        assertFalse(spec.useTwoPaneNowPlaying)
        assertEquals(1, spec.listColumns)
    }

    @Test
    fun phoneLandscape_switchesToTwoPaneNowPlayingAndNavigationRail() {
        val spec = computeAdaptiveLayoutSpec(widthDp = 844, heightDp = 390)
        assertEquals(MuseifyWidthClass.EXPANDED, spec.widthClass)
        assertTrue(spec.isLandscape)
        assertTrue(spec.useNavigationRail)
        assertTrue(spec.useTwoPaneNowPlaying)
        assertEquals(2, spec.listColumns)
    }

    @Test
    fun tabletPortraitAndLandscape_useAdaptiveMultiColumnAndTwoPaneDetails() {
        val portraitTablet = computeAdaptiveLayoutSpec(widthDp = 800, heightDp = 1280)
        assertEquals(MuseifyWidthClass.MEDIUM, portraitTablet.widthClass)
        assertTrue(portraitTablet.useNavigationRail)
        assertTrue(portraitTablet.useTwoPaneDetails)
        assertFalse(portraitTablet.useTwoPaneNowPlaying)
        assertEquals(2, portraitTablet.listColumns)

        assertTrue(shouldUseTwoPanePlayer(widthDp = 1024, heightDp = 680))
        assertEquals(MuseifyWidthClass.COMPACT, classifyWidthClass(599))
        assertEquals(MuseifyWidthClass.MEDIUM, classifyWidthClass(600))
        assertEquals(MuseifyWidthClass.EXPANDED, classifyWidthClass(840))
    }
}
