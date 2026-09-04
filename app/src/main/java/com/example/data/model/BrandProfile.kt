package com.example.data.model

data class BrandProfile(
    val name: String,
    val tagline: String,
    val description: String,
    val logoIcon: String = "code",
    val primaryColorHex: String = "#6366F1",
    val secondaryColorHex: String = "#8B5CF6",
    val accentColorHex: String = "#F59E0B",
    val techStack: List<String> = listOf("TypeScript", "React", "Remotion", "ElevenLabs", "Tailwind"),
    val keyFeatures: List<String> = listOf(
        "One-command promo video production",
        "Dual aspect ratio (16:9 + 9:16)",
        "AI voiceover with emotional scene presets",
        "Automated codebase brand discovery"
    ),
    val targetAudience: String = "Software engineers, indie hackers & founders",
    val repoPathOrUrl: String = "AKCodez/promo-video-skill"
)
