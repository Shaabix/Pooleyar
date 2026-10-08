package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,   // < 600dp (Phones)
    MEDIUM,    // 600dp .. 840dp (Tablet Portrait, Foldables)
    EXPANDED   // > 840dp (Tablet Landscape, Desktop)
}

data class WindowSizeInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean
) {
    val isTablet: Boolean get() = widthSizeClass != WindowWidthSizeClass.COMPACT || screenWidthDp >= 600.dp
}

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val widthSizeClass = when {
        configuration.screenWidthDp < 600 -> WindowWidthSizeClass.COMPACT
        configuration.screenWidthDp < 840 -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }

    return WindowSizeInfo(
        widthSizeClass = widthSizeClass,
        screenWidthDp = screenWidth,
        screenHeightDp = screenHeight,
        isLandscape = isLandscape
    )
}
