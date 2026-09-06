package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// DEVDIRECTOR THEME PALETTES
// ============================================================

// --- 1. Studio Light Mode ---
// Warm white surfaces, dark readable text, indigo/violet controls
val StudioLightBg = Color(0xFFF8F9FC)
val StudioLightSurface = Color(0xFFFFFFFF)
val StudioLightSurfaceVariant = Color(0xFFEEF2F6)
val StudioLightSurfaceCard = Color(0xFFFFFFFF)
val StudioLightSurfaceCardElevated = Color(0xFFF1F5F9)
val StudioLightTextPrimary = Color(0xFF0F172A)
val StudioLightTextSecondary = Color(0xFF475569)
val StudioLightTextMuted = Color(0xFF94A3B8)
val StudioLightPrimary = Color(0xFF6366F1) // Indigo 500
val StudioLightPrimaryDark = Color(0xFF4F46E5) // Indigo 600
val StudioLightPrimaryLight = Color(0xFF818CF8) // Indigo 400
val StudioLightSecondary = Color(0xFF8B5CF6) // Violet 500
val StudioLightSecondaryContainer = Color(0xFFEDE9FE)
val StudioLightOutline = Color(0xFFCBD5E1) // Slate 300
val StudioLightOutlineVariant = Color(0xFFC7D2FE) // Indigo 200

// --- 2. Studio Dark Mode ---
// Near-black cinematic editing workspace, restrained violet/cyan accents
val StudioDarkBg = Color(0xFF08080A)
val StudioDarkSurface = Color(0xFF121216)
val StudioDarkSurfaceVariant = Color(0xFF18181F)
val StudioDarkSurfaceCard = Color(0xFF14141A)
val StudioDarkSurfaceCardElevated = Color(0xFF1E1E26)
val StudioDarkTextPrimary = Color(0xFFFFFFFF)
val StudioDarkTextSecondary = Color(0xFFCBD5E1)
val StudioDarkTextMuted = Color(0xFF64748B)
val StudioDarkPrimary = Color(0xFF7C3AED) // Violet 600
val StudioDarkPrimaryLight = Color(0xFF8B5CF6)
val StudioDarkPrimaryDark = Color(0xFF6D28D9)
val StudioDarkSecondary = Color(0xFF06B6D4) // Restrained Cyan
val StudioDarkSecondaryContainer = Color(0xFF2E1065)
val StudioDarkOutline = Color(0x1AFFFFFF)
val StudioDarkOutlineVariant = Color(0x408B5CF6)

// --- 3. Neon Director Mode ---
// Dark backdrop, cyan/violet/magenta accents, expressive but clean
val NeonDirectorBg = Color(0xFF060713)
val NeonDirectorSurface = Color(0xFF0F1123)
val NeonDirectorSurfaceVariant = Color(0xFF171A34)
val NeonDirectorSurfaceCard = Color(0xFF12142B)
val NeonDirectorSurfaceCardElevated = Color(0xFF1B1E3F)
val NeonDirectorTextPrimary = Color(0xFFFFFFFF)
val NeonDirectorTextSecondary = Color(0xFFBAE6FD) // Cyan tinted text
val NeonDirectorTextMuted = Color(0xFF64748B)
val NeonDirectorPrimary = Color(0xFF00E5FF) // Electric Cyan
val NeonDirectorPrimaryLight = Color(0xFF67E8F9)
val NeonDirectorPrimaryDark = Color(0xFF0891B2)
val NeonDirectorSecondary = Color(0xFFFF007F) // Neon Hot Magenta
val NeonDirectorSecondaryLight = Color(0xFFF472B6)
val NeonDirectorSecondaryContainer = Color(0xFF3B0764)
val NeonDirectorTertiary = Color(0xFF8B5CF6) // Neon Violet
val NeonDirectorOutline = Color(0x3300E5FF)
val NeonDirectorOutlineVariant = Color(0x66FF007F)

// --- 4. Cyber Director Mode ---
// Black/navy background with magenta/cyan lighting accents, strong selected-state treatment
val CyberDirectorBg = Color(0xFF030712) // Very dark navy/black
val CyberDirectorSurface = Color(0xFF0B132B) // Cyber Navy
val CyberDirectorSurfaceVariant = Color(0xFF1C2541)
val CyberDirectorSurfaceCard = Color(0xFF0D1B2A)
val CyberDirectorSurfaceCardElevated = Color(0xFF1E293B)
val CyberDirectorTextPrimary = Color(0xFFFFFFFF)
val CyberDirectorTextSecondary = Color(0xFFE2E8F0)
val CyberDirectorTextMuted = Color(0xFF718096)
val CyberDirectorPrimary = Color(0xFFFF0055) // Laser Magenta
val CyberDirectorPrimaryLight = Color(0xFFFB7185)
val CyberDirectorPrimaryDark = Color(0xFFBE123C)
val CyberDirectorSecondary = Color(0xFF00F0FF) // Cyber Cyan Lighting
val CyberDirectorSecondaryLight = Color(0xFF38BDF8)
val CyberDirectorSecondaryContainer = Color(0xFF1E1B4B)
val CyberDirectorTertiary = Color(0xFFF59E0B) // Amber Glow
val CyberDirectorOutline = Color(0x4000F0FF)
val CyberDirectorOutlineVariant = Color(0x80FF0055)

// ============================================================
// LEGACY / SHARED BRAND TOKENS (Preserved for compatibility)
// ============================================================
val BackgroundDark = StudioDarkBg
val SurfaceDark = StudioDarkSurface
val SurfaceVariantDark = StudioDarkSurfaceVariant
val SurfaceCard = StudioDarkSurfaceCard
val SurfaceCardElevated = StudioDarkSurfaceCardElevated

val PrimaryIndigo = Color(0xFF7C3AED)
val PrimaryLight = Color(0xFF8B5CF6)
val PrimaryDark = Color(0xFF6D28D9)

val SecondaryViolet = Color(0xFF8B5CF6)
val SecondaryLight = Color(0xFFA78BFA)
val VioletSubtle = Color(0xFFC4B5FD)

val AccentAmber = Color(0xFFF59E0B)
val AccentOrange = Color(0xFFF97316)
val AccentRed = Color(0xFFDC2626)
val AccentRose = Color(0xFFF43F5E)
val AccentCyan = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldLight = Color(0xFF34D399)

val MetallicSilver = Color(0xFFCBD5E1)
val MetallicGold = Color(0xFFFBBF24)

val GlassBackground = Color(0x0DFFFFFF)
val GlassBorder = Color(0x1AFFFFFF)
val BrandGlow = Color(0x808B5CF6)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF64748B)

val BorderSubtle = Color(0x1AFFFFFF)
val BorderHighlight = Color(0x408B5CF6)

val TerminalBg = Color(0xFF0A0A0C)
val TerminalBorder = Color(0x26FFFFFF)
val CodeKeyword = Color(0xFFFF7B72)
val CodeString = Color(0xFFA5D6FF)
val CodeFunction = Color(0xFFD2A8FF)
val CodeComment = Color(0xFF8B949E)
val CodeVariable = Color(0xFF79C0FF)
