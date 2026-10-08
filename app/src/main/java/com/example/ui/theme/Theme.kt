package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = PersianEmeraldLight,
    onPrimary = Color.Black,
    primaryContainer = PersianEmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = PersianGoldAccent,
    onSecondary = Color.Black,
    background = PersianNavyBgDark,
    onBackground = TextPrimaryDark,
    surface = PersianNavySurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = PersianNavyCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF334E68),
    error = PersianRoseExpense,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PersianEmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = PersianEmeraldDark,
    secondary = PersianGoldAccent,
    onSecondary = Color.Black,
    background = PersianSurfaceLight,
    onBackground = TextPrimaryLight,
    surface = PersianCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = PersianOutlineLight,
    error = PersianRoseExpense,
    onError = Color.White
)

@Composable
fun PooleyarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
