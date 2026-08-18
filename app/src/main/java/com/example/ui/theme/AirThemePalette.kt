package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AirThemePalette(
    val title: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val border: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val qrBackground: Int,
    val qrForeground: Int
) {
    CYBER_SLATE(
        title = "Cyber Slate",
        description = "Deep slate with neon cyan & emerald (Standard)",
        background = Color(0xFF020617),
        surface = Color(0xFF0F172A),
        border = Color(0xFF1E293B),
        primaryAccent = Color(0xFF22D3EE),
        secondaryAccent = Color(0xFF34D399),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        qrBackground = 0xFFFFFFFF.toInt(),
        qrForeground = 0xFF000000.toInt()
    ),
    SUNLIGHT_GLARE(
        title = "Sunlight / Glare Mode",
        description = "Ultra high-contrast for bright outdoor daylight",
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF171717),
        border = Color(0xFFF59E0B),
        primaryAccent = Color(0xFFFBBF24),
        secondaryAccent = Color(0xFF38BDF8),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD4D4D8),
        qrBackground = 0xFFFFFFFF.toInt(),
        qrForeground = 0xFF000000.toInt()
    ),
    OLED_MIDNIGHT(
        title = "OLED Midnight",
        description = "Pitch black #000000 for maximum OLED contrast & power saving",
        background = Color(0xFF000000),
        surface = Color(0xFF09090B),
        border = Color(0xFF27272A),
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF06B6D4),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFFA1A1AA),
        qrBackground = 0xFFFFFFFF.toInt(),
        qrForeground = 0xFF000000.toInt()
    ),
    TACTICAL_AMBER(
        title = "Tactical Night Vision",
        description = "Low-light / night vision eye-safe high-contrast monochrome",
        background = Color(0xFF0C0A09),
        surface = Color(0xFF1C1917),
        border = Color(0xFFD97706),
        primaryAccent = Color(0xFFF59E0B),
        secondaryAccent = Color(0xFFEA580C),
        textPrimary = Color(0xFFFEF3C7),
        textSecondary = Color(0xFFD97706),
        qrBackground = 0xFF000000.toInt(),
        qrForeground = 0xFFF59E0B.toInt()
    )
}
