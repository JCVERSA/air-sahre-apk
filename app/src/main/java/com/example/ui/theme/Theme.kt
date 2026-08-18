package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = Slate950,
    primaryContainer = Cyan900,
    onPrimaryContainer = Cyan400,
    secondary = Purple400,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = Purple400,
    tertiary = Emerald400,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = Red500,
    onError = Color.White
)

@Composable
fun AirQRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // AirQR uses high-contrast dark aesthetic by default for optical air-gap clarity
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
