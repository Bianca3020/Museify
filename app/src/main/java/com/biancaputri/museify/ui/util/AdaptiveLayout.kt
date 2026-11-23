package com.biancaputri.museify.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MuseifyWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class AdaptiveLayoutSpec(
    val widthClass: MuseifyWidthClass,
    val isLandscape: Boolean,
    val isSmallPhone: Boolean,
    val useNavigationRail: Boolean,
    val useTwoPaneNowPlaying: Boolean,
    val useTwoPaneDetails: Boolean,
    val listColumns: Int,
    val horizontalPadding: Dp,
    val featuredCardWidth: Dp,
    val compactCardWidth: Dp
)

fun classifyWidthClass(widthDp: Int): MuseifyWidthClass {
    return when {
        widthDp >= 840 -> MuseifyWidthClass.EXPANDED
        widthDp >= 600 -> MuseifyWidthClass.MEDIUM
        else -> MuseifyWidthClass.COMPACT
    }
}

fun shouldUseTwoPanePlayer(widthDp: Int, heightDp: Int): Boolean {
    val isLandscape = widthDp > heightDp
    return (isLandscape && widthDp >= 560) || (widthDp >= 720 && heightDp < 720)
}

fun computeAdaptiveLayoutSpec(widthDp: Int, heightDp: Int): AdaptiveLayoutSpec {
    val widthClass = classifyWidthClass(widthDp)
    val isLandscape = widthDp > heightDp
    val isSmallPhone = widthDp < 360

    val horizontalPadding = when (widthClass) {
        MuseifyWidthClass.COMPACT -> if (isSmallPhone) 16.dp else 20.dp
        MuseifyWidthClass.MEDIUM -> 24.dp
        MuseifyWidthClass.EXPANDED -> 32.dp
    }

    val featuredCardWidth = when {
        isSmallPhone -> 212.dp
        widthClass == MuseifyWidthClass.COMPACT -> 244.dp
        widthClass == MuseifyWidthClass.MEDIUM -> 260.dp
        else -> 280.dp
    }

    val compactCardWidth = when {
        isSmallPhone -> 136.dp
        widthClass == MuseifyWidthClass.COMPACT -> 150.dp
        else -> 164.dp
    }

    val listColumns = when (widthClass) {
        MuseifyWidthClass.COMPACT -> 1
        MuseifyWidthClass.MEDIUM -> 2
        MuseifyWidthClass.EXPANDED -> 2
    }

    return AdaptiveLayoutSpec(
        widthClass = widthClass,
        isLandscape = isLandscape,
        isSmallPhone = isSmallPhone,
        useNavigationRail = widthClass != MuseifyWidthClass.COMPACT,
        useTwoPaneNowPlaying = shouldUseTwoPanePlayer(widthDp, heightDp),
        useTwoPaneDetails = widthDp >= 640,
        listColumns = listColumns,
        horizontalPadding = horizontalPadding,
        featuredCardWidth = featuredCardWidth,
        compactCardWidth = compactCardWidth
    )
}
