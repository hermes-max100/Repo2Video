package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo, // Violet 600
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = SecondaryLight,
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = BackgroundDark, // #050505
    onBackground = TextPrimary,
    surface = SurfaceDark, // #121214
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark, // #1A1A1E
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle, // white/10
    outlineVariant = BorderHighlight // violet glow border
)

private val LightColorScheme = DarkColorScheme // Always use immersive dark studio aesthetic

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Applies the Immersive UI dual-radial gradient atmosphere:
 * - Top ambient violet glow: radial-gradient(circle at 50% 0%, rgba(76, 29, 149, 0.25) 0%, transparent 70%)
 * - Bottom-left warm dusk glow: radial-gradient(circle at 10% 80%, rgba(249, 115, 22, 0.1) 0%, transparent 40%)
 * - Base obsidian canvas: #050505
 */
fun Modifier.immersiveGradientBackground(): Modifier = this.drawBehind {
    // 1. Obsidian solid base
    drawRect(Color(0xFF050505))

    // 2. Top-center radial violet glow
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

    // 3. Bottom-left warm orange ember accent
    val bottomCenter = Offset(size.width * 0.1f, size.height * 0.82f)
    val bottomRadius = size.width * 0.85f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x1EF97316), // Orange-500 glow 12%
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
