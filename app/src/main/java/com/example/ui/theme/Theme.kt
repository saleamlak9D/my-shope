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
    primary = Color(0xFF85E0A3),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF0D5329),
    onPrimaryContainer = Color(0xFFC4F2D6),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF1B4D3E),
    onSecondaryContainer = Color(0xFFD4ECD8),
    tertiary = Color(0xFF8AB4F8),
    background = Color(0xFF101412),
    surface = Color(0xFF171C19),
    surfaceVariant = Color(0xFF232A26),
    onSurface = Color(0xFFE8ECE9),
    onSurfaceVariant = Color(0xFFC1C9C3),
    outline = Color(0xFF38433C),
    error = Color(0xFFF2B8B5)
)

private val LightColorScheme = lightColorScheme(
    primary = SheetsForestGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5E1),
    onPrimaryContainer = Color(0xFF003916),
    secondary = SheetsDarkGreen40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4F3E8),
    onSecondaryContainer = Color(0xFF0A3818),
    tertiary = SheetsAccentBlue,
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF5F1),
    onSurface = Color(0xFF141916),
    onSurfaceVariant = Color(0xFF49544D),
    outline = Color(0xFFD0DCD4),
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
