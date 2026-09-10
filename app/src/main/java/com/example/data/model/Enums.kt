package com.example.data.model

enum class NarrativeTemplate(
    val title: String,
    val arc: String,
    val bestFor: String,
    val defaultSceneCount: Int
) {
    RAGE_HOOK(
        title = "The Rage Hook",
        arc = "Frustration → Silence → Whisper → Reveal → CTA",
        bestFor = "Products solving a painful developer or user problem",
        defaultSceneCount = 5
    ),
    PROBLEM_STACK(
        title = "The Problem Stack",
        arc = "Pain → Pain → Pain → \"What if...\" → Solution → CTA",
        bestFor = "Products with multiple pain points to address",
        defaultSceneCount = 6
    ),
    DEMO_FIRST(
        title = "The Demo First",
        arc = "Magic moment → \"How?\" → Features → CTA",
        bestFor = "Products where the UX sells itself immediately",
        defaultSceneCount = 4
    ),
    TRANSFORMATION(
        title = "The Transformation",
        arc = "Before → After → How → Proof → CTA",
        bestFor = "Workflow improvements & productivity multipliers",
        defaultSceneCount = 5
    )
}

enum class VoiceActor(
    val voiceName: String,
    val style: String,
    val bestFor: String,
    val isMale: Boolean,
    val basePitch: Float,
    val baseRate: Float,
    val elevenLabsVoiceId: String
) {
    MATILDA(
        voiceName = "Matilda",
        style = "Warm, confident female",
        bestFor = "Default — versatile for any SaaS",
        isMale = false,
        basePitch = 1.1f,
        baseRate = 1.05f,
        elevenLabsVoiceId = "XrExE9yKIg1WjnnlVkGX"
    ),
    RACHEL(
        voiceName = "Rachel",
        style = "Calm, authoritative female",
        bestFor = "Corporate, B2B & enterprise tools",
        isMale = false,
        basePitch = 1.0f,
        baseRate = 0.98f,
        elevenLabsVoiceId = "21m00Tcm4TlvDq8ikWAM"
    ),
    DANIEL(
        voiceName = "Daniel",
        style = "Polished, broadcast male",
        bestFor = "Advertising & product launches",
        isMale = true,
        basePitch = 0.92f,
        baseRate = 1.02f,
        elevenLabsVoiceId = "onwK4e9ZLuTAKqWW03F9"
    ),
    JOSH(
        voiceName = "Josh",
        style = "Friendly, conversational male",
        bestFor = "Consumer apps & indie dev tools",
        isMale = true,
        basePitch = 1.0f,
        baseRate = 1.1f,
        elevenLabsVoiceId = "TxGEqnHWrfWFTfGW9XjX"
    ),
    ADAM(
        voiceName = "Adam",
        style = "Deep, dramatic male",
        bestFor = "Cinematic, intense hooks & punchy ads",
        isMale = true,
        basePitch = 0.82f,
        baseRate = 0.95f,
        elevenLabsVoiceId = "pNInz6obpgDQGcFmaJgB"
    )
}

enum class EmotionalPreset(
    val label: String,
    val description: String,
    val sampleQuote: String,
    val pitchMultiplier: Float,
    val rateMultiplier: Float,
    val elevenLabsStability: Float = 0.50f,
    val elevenLabsSimilarityBoost: Float = 0.80f,
    val elevenLabsStyle: Float = 0.20f,
    val elevenLabsSpeakerBoost: Boolean = true
) {
    RAGE(
        label = "Rage / Frustration",
        description = "Low stability, high urgency",
        sampleQuote = "\"Are you serious right now?! Why is this so hard?\"",
        pitchMultiplier = 1.25f,
        rateMultiplier = 1.2f,
        elevenLabsStability = 0.20f,
        elevenLabsSimilarityBoost = 0.85f,
        elevenLabsStyle = 0.55f,
        elevenLabsSpeakerBoost = true
    ),
    WHISPER(
        label = "Whisper / Secret",
        description = "Intimate, low stability, confidential",
        sampleQuote = "\"What if you never had to guess again?\"",
        pitchMultiplier = 0.85f,
        rateMultiplier = 0.88f,
        elevenLabsStability = 0.35f,
        elevenLabsSimilarityBoost = 0.80f,
        elevenLabsStyle = 0.30f,
        elevenLabsSpeakerBoost = true
    ),
    CONFIDENT(
        label = "Confident",
        description = "Mid stability, balanced, authoritative",
        sampleQuote = "\"Smart detection scans and builds in seconds.\"",
        pitchMultiplier = 1.0f,
        rateMultiplier = 1.0f,
        elevenLabsStability = 0.55f,
        elevenLabsSimilarityBoost = 0.80f,
        elevenLabsStyle = 0.15f,
        elevenLabsSpeakerBoost = true
    ),
    WARM(
        label = "Warm",
        description = "High stability, smooth cadence",
        sampleQuote = "\"Join over 50,000 developers building faster.\"",
        pitchMultiplier = 1.05f,
        rateMultiplier = 0.96f,
        elevenLabsStability = 0.70f,
        elevenLabsSimilarityBoost = 0.82f,
        elevenLabsStyle = 0.10f,
        elevenLabsSpeakerBoost = true
    ),
    DRAMATIC(
        label = "Dramatic",
        description = "High style, punchy & resonant",
        sampleQuote = "\"Try it right now. Ship in seconds.\"",
        pitchMultiplier = 0.88f,
        rateMultiplier = 0.92f,
        elevenLabsStability = 0.28f,
        elevenLabsSimilarityBoost = 0.88f,
        elevenLabsStyle = 0.50f,
        elevenLabsSpeakerBoost = true
    )
}

enum class TransitionStyle(
    val displayName: String,
    val description: String
) {
    METALLIC_SWOOSH(
        displayName = "Metallic Swoosh",
        description = "Signature chrome metallic gleam sweep with velocity blur"
    ),
    SPRING_WIPE(
        displayName = "Spring Wipe",
        description = "Snappy physics-based elastic wipe"
    ),
    VELOCITY_SLIDE(
        displayName = "Velocity Slide",
        description = "High-speed directional kinetic push"
    ),
    CINEMATIC_FADE(
        displayName = "Smooth Fade",
        description = "Atmospheric filmic cross-dissolve"
    )
}

enum class MusicTrackOption(
    val trackTitle: String,
    val vibe: String,
    val bpm: Int
) {
    INSPIRED_AMBIENT(
        trackTitle = "Inspired Ambient",
        vibe = "Ambient, atmospheric & futuristic",
        bpm = 110
    ),
    MOTIVATIONAL_DAY(
        trackTitle = "Motivational Day",
        vibe = "Commercial, uplifting & melodic",
        bpm = 124
    ),
    UPBEAT_CORPORATE(
        trackTitle = "Upbeat Corporate",
        vibe = "Inspiring, energetic & crisp",
        bpm = 120
    ),
    NONE(
        trackTitle = "Mute Music",
        vibe = "Voiceover only",
        bpm = 0
    )
}

enum class AspectRatioFormat(
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int,
    val aspectFloat: Float
) {
    LANDSCAPE_16_9("16:9", "Landscape (1920 × 1080) - YouTube & Web", 1920, 1080, 16f / 9f),
    PORTRAIT_9_16("9:16", "Portrait (1080 × 1920) - TikTok, Reels, Shorts", 1080, 1920, 9f / 16f),
    SQUARE_1_1("1:1", "Square (1080 × 1080) - Feed & Carousel", 1080, 1080, 1.0f);

    val displayName: String get() = label
}

enum class SceneVisualType(
    val displayName: String,
    val description: String
) {
    HOOK_FRUSTRATION("Hook Frustration", "Moody vignette, shaky rage typography, dramatic warning pulse"),
    CODE_TERMINAL("Code Terminal", "Animated syntax-highlighted IDE window with live typing"),
    BROWSER_MOCKUP_3D("3D Browser Mockup", "Tilted 3D browser frame scrolling live application preview"),
    FEATURE_SPOTLIGHT("Feature Spotlight", "Floating 3D glass cards with neon glowing feature badges"),
    METRIC_COUNTER("Metric Counter", "Rapid counter animation with performance proof badges"),
    LOGO_REVEAL_CTA("Logo Reveal & CTA", "Radial starburst particle glow, logo scale spring, CTA button"),
    VEO_AI_VIDEO("Veo AI Clip", "Cinematic AI-generated background video animation")
}

enum class AiEngine(
    val title: String,
    val modelName: String,
    val badgeLabel: String,
    val description: String
) {
    GEMINI(
        title = "Google Gemini",
        modelName = "Gemini 2.5 Flash",
        badgeLabel = "Gemini AI",
        description = "Multimodal reasoning & structured storyboards"
    ),
    GROK(
        title = "xAI Grok",
        modelName = "Grok 2 (SuperGrok)",
        badgeLabel = "SuperGrok",
        description = "High-energy, witty developer hooks via OAuth"
    )
}
