package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// MATERIAL 3 COLOR SCHEMES FOR DEVDIRECTOR
// ============================================================

// 1. Studio Light: warm white surfaces, dark readable text, indigo/violet controls
val StudioLightColorScheme: ColorScheme = lightColorScheme(
    primary = StudioLightPrimary,
    onPrimary = Color.White,
    primaryContainer = StudioLightSecondaryContainer,
    onPrimaryContainer = Color(0xFF4338CA),
    secondary = StudioLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = AccentCyan,
    onTertiary = Color.White,
    background = StudioLightBg,
    onBackground = StudioLightTextPrimary,
    surface = StudioLightSurface,
    onSurface = StudioLightTextPrimary,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = StudioLightTextSecondary,
    outline = StudioLightOutline,
    outlineVariant = StudioLightOutlineVariant
)

// 2. Studio Dark: near-black cinematic editing workspace, restrained violet/cyan accents
val StudioDarkColorScheme: ColorScheme = darkColorScheme(
    primary = StudioDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = StudioDarkPrimaryDark,
    onPrimaryContainer = StudioDarkPrimaryLight,
    secondary = StudioDarkSecondary,
    onSecondary = Color.White,
    secondaryContainer = StudioDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFC4B5FD),
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = StudioDarkTextPrimary,
    surface = StudioDarkSurface,
    onSurface = StudioDarkTextPrimary,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = StudioDarkTextSecondary,
    outline = StudioDarkOutline,
    outlineVariant = StudioDarkOutlineVariant
)

// 3. Neon Director: dark backdrop, cyan/violet/magenta accents, expressive but clean
val NeonDirectorColorScheme: ColorScheme = darkColorScheme(
    primary = NeonDirectorPrimary,
    onPrimary = Color.Black,
    primaryContainer = NeonDirectorPrimaryDark,
    onPrimaryContainer = NeonDirectorPrimaryLight,
    secondary = NeonDirectorSecondary,
    onSecondary = Color.White,
    secondaryContainer = NeonDirectorSecondaryContainer,
    onSecondaryContainer = NeonDirectorSecondaryLight,
    tertiary = NeonDirectorTertiary,
    onTertiary = Color.White,
    background = NeonDirectorBg,
    onBackground = NeonDirectorTextPrimary,
    surface = NeonDirectorSurface,
    onSurface = NeonDirectorTextPrimary,
    surfaceVariant = NeonDirectorSurfaceVariant,
    onSurfaceVariant = NeonDirectorTextSecondary,
    outline = NeonDirectorOutline,
    outlineVariant = NeonDirectorOutlineVariant
)

// 4. Cyber Director: black/navy background with magenta/cyan lighting accents
val CyberDirectorColorScheme: ColorScheme = darkColorScheme(
    primary = CyberDirectorPrimary,
    onPrimary = Color.White,
    primaryContainer = CyberDirectorPrimaryDark,
    onPrimaryContainer = CyberDirectorPrimaryLight,
    secondary = CyberDirectorSecondary,
    onSecondary = Color.Black,
    secondaryContainer = CyberDirectorSecondaryContainer,
    onSecondaryContainer = CyberDirectorSecondaryLight,
    tertiary = CyberDirectorTertiary,
    onTertiary = Color.Black,
    background = CyberDirectorBg,
    onBackground = CyberDirectorTextPrimary,
    surface = CyberDirectorSurface,
    onSurface = CyberDirectorTextPrimary,
    surfaceVariant = CyberDirectorSurfaceVariant,
    onSurfaceVariant = CyberDirectorTextSecondary,
    outline = CyberDirectorOutline,
    outlineVariant = CyberDirectorOutlineVariant
)

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }

/**
 * Top-level application theme for DevDirector.
 * Automatically resolves the active color scheme according to the persisted theme mode.
 */
@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activeColorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
        AppThemeMode.STUDIO_LIGHT -> StudioLightColorScheme
        AppThemeMode.STUDIO_DARK -> StudioDarkColorScheme
        AppThemeMode.NEON_DIRECTOR -> NeonDirectorColorScheme
        AppThemeMode.CYBER_DIRECTOR -> CyberDirectorColorScheme
    }

    CompositionLocalProvider(
        LocalAppThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = activeColorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Applies theme-aware atmospheric background styling.
 */
fun Modifier.immersiveGradientBackground(
    themeMode: AppThemeMode = AppThemeMode.STUDIO_DARK,
    isDarkDevice: Boolean = true
): Modifier = this.drawBehind {
    when (themeMode) {
        AppThemeMode.STUDIO_LIGHT -> {
            // Warm white base with subtle indigo and amber accents
            drawRect(StudioLightBg)
            val topCenter = Offset(size.width * 0.5f, 0f)
            val topRadius = (size.height.coerceAtLeast(size.width)) * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x186366F1), // Soft indigo
                        Color(0x088B5CF6),
                        Color.Transparent
                    ),
                    center = topCenter,
                    radius = topRadius
                ),
                radius = topRadius,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.1f, size.height * 0.85f)
            val bottomRadius = size.width * 0.8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x0EF59E0B), // Soft amber
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = bottomRadius
                ),
                radius = bottomRadius,
                center = bottomCenter
            )
        }

        AppThemeMode.STUDIO_DARK -> {
            // Obsidian base with ambient violet glow and dusk ember
            drawRect(StudioDarkBg)
            val topCenter = Offset(size.width * 0.5f, 0f)
            val topRadius = (size.height.coerceAtLeast(size.width)) * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x404C1D95), // Deep violet glow
                        Color(0x207C3AED),
                        Color.Transparent
                    ),
                    center = topCenter,
                    radius = topRadius
                ),
                radius = topRadius,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.1f, size.height * 0.82f)
            val bottomRadius = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x1EF97316), // Warm orange ember
                        Color(0x0AF97316),
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = bottomRadius
                ),
                radius = bottomRadius,
                center = bottomCenter
            )
        }

        AppThemeMode.NEON_DIRECTOR -> {
            // Deep midnight with electric cyan and hot magenta neon aura
            drawRect(NeonDirectorBg)
            val topCenter = Offset(size.width * 0.5f, 0f)
            val topRadius = (size.height.coerceAtLeast(size.width)) * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3500E5FF), // Electric cyan
                        Color(0x158B5CF6),
                        Color.Transparent
                    ),
                    center = topCenter,
                    radius = topRadius
                ),
                radius = topRadius,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.9f, size.height * 0.85f)
            val bottomRadius = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28FF007F), // Hot neon magenta
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = bottomRadius
                ),
                radius = bottomRadius,
                center = bottomCenter
            )
        }

        AppThemeMode.CYBER_DIRECTOR -> {
            // Black/navy with cyber magenta overhead laser and cyber cyan accents
            drawRect(CyberDirectorBg)
            val topCenter = Offset(size.width * 0.5f, 0f)
            val topRadius = (size.height.coerceAtLeast(size.width)) * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x35FF0055), // Laser magenta
                        Color(0x150F172A),
                        Color.Transparent
                    ),
                    center = topCenter,
                    radius = topRadius
                ),
                radius = topRadius,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.15f, size.height * 0.82f)
            val bottomRadius = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2500F0FF), // Cyber cyan
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = bottomRadius
                ),
                radius = bottomRadius,
                center = bottomCenter
            )
        }

        AppThemeMode.SYSTEM -> {
            // Respect system dark/light state
            if (isDarkDevice) {
                drawRect(StudioDarkBg)
                val topCenter = Offset(size.width * 0.5f, 0f)
                val topRadius = (size.height.coerceAtLeast(size.width)) * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x404C1D95),
                            Color(0x207C3AED),
                            Color.Transparent
                        ),
                        center = topCenter,
                        radius = topRadius
                    ),
                    radius = topRadius,
                    center = topCenter
                )
            } else {
                drawRect(StudioLightBg)
                val topCenter = Offset(size.width * 0.5f, 0f)
                val topRadius = (size.height.coerceAtLeast(size.width)) * 0.7f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x186366F1),
                            Color.Transparent
                        ),
                        center = topCenter,
                        radius = topRadius
                    ),
                    radius = topRadius,
                    center = topCenter
                )
            }
        }
    }
}

/**
 * Subtle technical dot matrix background for video preview canvases and mockups
 */
fun Modifier.dotMatrix(
    dotColor: Color = Color(0x2E4F46E5),
    spacingPx: Float = 22f,
    dotRadiusPx: Float = 1.2f
): Modifier = this.drawBehind {
    var x = spacingPx / 2
    while (x < size.width) {
        var y = spacingPx / 2
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = dotRadiusPx,
                center = Offset(x, y)
            )
            y += spacingPx
        }
        x += spacingPx
    }
}
