package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val id: String,
    val displayName: String,
    val description: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewBackground: Color,
    val previewSurface: Color
) {
    SYSTEM(
        id = "system",
        displayName = "System",
        description = "Matches device dark or light appearance automatically",
        previewPrimary = Color(0xFF7C3AED),
        previewSecondary = Color(0xFF8B5CF6),
        previewBackground = Color(0xFF121214),
        previewSurface = Color(0xFF1A1A1E)
    ),
    STUDIO_LIGHT(
        id = "studio_light",
        displayName = "Studio Light",
        description = "Warm white surfaces, dark readable text, indigo/violet controls",
        previewPrimary = Color(0xFF6366F1),
        previewSecondary = Color(0xFF8B5CF6),
        previewBackground = Color(0xFFF8F9FC),
        previewSurface = Color(0xFFFFFFFF)
    ),
    STUDIO_DARK(
        id = "studio_dark",
        displayName = "Studio Dark",
        description = "Near-black cinematic editing workspace with restrained violet/cyan accents",
        previewPrimary = Color(0xFF7C3AED),
        previewSecondary = Color(0xFF06B6D4),
        previewBackground = Color(0xFF08080A),
        previewSurface = Color(0xFF121216)
    ),
    NEON_DIRECTOR(
        id = "neon_director",
        displayName = "Neon Director",
        description = "Deep dark backdrop with glowing cyan, violet & magenta accents",
        previewPrimary = Color(0xFF00E5FF),
        previewSecondary = Color(0xFFFF007F),
        previewBackground = Color(0xFF060713),
        previewSurface = Color(0xFF0F1123)
    ),
    CYBER_DIRECTOR(
        id = "cyber_director",
        displayName = "Cyber Director",
        description = "Black/navy background with cyber magenta/cyan lighting accents",
        previewPrimary = Color(0xFFFF0055),
        previewSecondary = Color(0xFF00F0FF),
        previewBackground = Color(0xFF030712),
        previewSurface = Color(0xFF0F172A)
    );

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}
