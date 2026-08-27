package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PolishDarkPrimary,
    onPrimary = PolishDarkOnPrimary,
    primaryContainer = PolishDarkPrimaryContainer,
    onPrimaryContainer = PolishDarkOnPrimaryContainer,
    secondary = PolishDarkSecondary,
    onSecondary = PolishDarkOnSecondary,
    secondaryContainer = PolishDarkSecondaryContainer,
    onSecondaryContainer = PolishDarkOnSecondaryContainer,
    tertiary = PolishDarkTertiary,
    onTertiary = PolishDarkOnTertiary,
    tertiaryContainer = PolishDarkTertiaryContainer,
    onTertiaryContainer = PolishDarkOnTertiaryContainer,
    background = PolishDarkBackground,
    onBackground = PolishDarkOnBackground,
    surface = PolishDarkSurface,
    onSurface = PolishDarkOnSurface,
    surfaceVariant = PolishDarkSurfaceVariant,
    onSurfaceVariant = PolishDarkOnSurfaceVariant,
    outline = PolishDarkOutline,
    outlineVariant = PolishDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishOnPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondary = PolishSecondary,
    onSecondary = PolishOnSecondary,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    tertiary = PolishTertiary,
    onTertiary = PolishOnTertiary,
    tertiaryContainer = PolishTertiaryContainer,
    onTertiaryContainer = PolishOnTertiaryContainer,
    background = PolishBackground,
    onBackground = PolishOnBackground,
    surface = PolishSurface,
    onSurface = PolishOnSurface,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outline = PolishOutline,
    outlineVariant = PolishOutlineVariant
)

@Composable
fun AntarStoreTheme(
    palette: AppThemePalette = AppThemePalette.PURPLE,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> palette.darkColorScheme
        else -> palette.lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    palette: AppThemePalette = AppThemePalette.PURPLE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AntarStoreTheme(palette = palette, darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
