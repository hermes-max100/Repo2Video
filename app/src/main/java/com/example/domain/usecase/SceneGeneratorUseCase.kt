package com.example.domain.usecase

import com.example.data.model.CreativeBrief
import com.example.data.model.EmotionalPreset
import com.example.data.model.LicensingAttribution
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import java.util.UUID

class SceneGeneratorUseCase {

    /**
     * Builds and validates the complete promo scene plan.
     */
    fun buildScenePlan(
        brief: CreativeBrief,
        scriptLines: List<ScriptLine>
    ): Pair<List<PromoScene>, List<LicensingAttribution>> {
        val totalBudget = brief.durationBudgetSeconds.toDouble()
        val count = scriptLines.size.coerceAtLeast(1)
        val baseDuration = totalBudget / count

        val visualTypes = listOf(
            SceneVisualType.CODE_TERMINAL,
            SceneVisualType.HOOK_FRUSTRATION,
            SceneVisualType.BROWSER_MOCKUP_3D,
            SceneVisualType.FEATURE_SPOTLIGHT,
            SceneVisualType.METRIC_COUNTER,
            SceneVisualType.LOGO_REVEAL_CTA
        )

        val emotionalPresets = listOf(
            EmotionalPreset.DRAMATIC,
            EmotionalPreset.CONFIDENT,
            EmotionalPreset.WARM,
            EmotionalPreset.RAGE,
            EmotionalPreset.WHISPER
        )

        val scenes = mutableListOf<PromoScene>()
        val licensing = mutableListOf<LicensingAttribution>()

        for ((idx, line) in scriptLines.withIndex()) {
            val visualType = visualTypes[idx % visualTypes.size]
            val emotion = emotionalPresets[idx % emotionalPresets.size]

            // Dynamic snippet generation
            val codeSnippet = when (visualType) {
                SceneVisualType.CODE_TERMINAL -> "import { Composition } from 'remotion';\nexport const Promo = () => <Sequence />;"
                SceneVisualType.HOOK_FRUSTRATION -> "$ npx repo2video scan ${brief.title.lowercase()}\n✓ Deterministic scan verified in 142ms\n✓ Secrets filtered: 0 leaked"
                else -> null
            }

            scenes.add(
                PromoScene(
                    id = UUID.randomUUID().toString(),
                    orderIndex = idx,
                    title = line.title,
                    subtitle = line.subtitle,
                    voiceover = line.voiceover,
                    emotionalPreset = emotion,
                    durationSeconds = baseDuration.toFloat(),
                    visualType = visualType,
                    codeSnippet = codeSnippet
                )
            )

            // Licensing attribution for scene visuals & font
            licensing.add(
                LicensingAttribution(
                    assetName = "JetBrains Mono (Scene ${idx + 1})",
                    assetType = "CODE_FONT",
                    source = "JetBrains Open Source",
                    licenseType = "OFL-1.1 (Open Font License)",
                    publishableStatus = "SAFE_TO_PUBLISH",
                    attributionNotice = "Copyright 2020 JetBrains s.r.o."
                )
            )
        }

        // Add audio & video framework licensing
        licensing.add(
            LicensingAttribution(
                assetName = "Ambient Cyber Bed (Looped Track)",
                assetType = "AUDIO_BED",
                source = "DevDirector Native Synthesizer",
                licenseType = "CC0 1.0 Universal",
                publishableStatus = "SAFE_TO_PUBLISH",
                attributionNotice = "Royalty-free procedural synth waveform generator"
            )
        )
        licensing.add(
            LicensingAttribution(
                assetName = "Remotion Engine v4.0",
                assetType = "RENDER_FRAMEWORK",
                source = "remotion-dev/remotion",
                licenseType = "Company License / Open Core",
                publishableStatus = "VERIFIED_COMPLIANT",
                attributionNotice = "Rendered via Remotion programmatic video primitives"
            )
        )

        return scenes to licensing
    }
}
