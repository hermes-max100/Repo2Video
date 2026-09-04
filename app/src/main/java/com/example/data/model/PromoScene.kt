package com.example.data.model

data class PromoScene(
    val id: String = java.util.UUID.randomUUID().toString(),
    val orderIndex: Int,
    val title: String,
    val subtitle: String = "",
    val voiceover: String,
    val emotionalPreset: EmotionalPreset = EmotionalPreset.CONFIDENT,
    val durationSeconds: Float = 3.5f,
    val visualType: SceneVisualType = SceneVisualType.BROWSER_MOCKUP_3D,
    val accentTag: String = "",
    val codeSnippet: String? = null,
    val metricNumber: String? = null,
    val metricLabel: String? = null,
    val badgeTags: List<String> = emptyList(),
    val ctaButtonText: String? = null,
    val ctaUrl: String? = null,
    val imagePrompt: String? = null,
    val generatedImageUrl: String? = null,
    val generatedVideoUrl: String? = null
)
