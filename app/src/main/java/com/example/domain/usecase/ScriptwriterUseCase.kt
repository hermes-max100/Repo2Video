package com.example.domain.usecase

import com.example.data.model.ClaimEvidence
import com.example.data.model.CreativeBrief
import com.example.data.model.NarrativeTemplate

data class ScriptLine(
    val sceneIndex: Int,
    val title: String,
    val subtitle: String,
    val voiceover: String,
    val linkedClaim: ClaimEvidence?,
    val isVerifiedClaim: Boolean,
    val characterCount: Int
)

class ScriptwriterUseCase {

    companion object {
        const val MAX_VOICEOVER_CHARS_PER_SCENE = 140
        const val TARGET_WORDS_PER_MINUTE = 160
    }

    /**
     * Generates script lines for each scene with explicit claim-evidence linking.
     */
    fun composeScript(
        brief: CreativeBrief,
        template: NarrativeTemplate,
        sceneCount: Int
    ): List<ScriptLine> {
        val scriptLines = mutableListOf<ScriptLine>()
        val claims = brief.keyClaims

        for (i in 0 until sceneCount) {
            val (title, subtitle, voiceover, claim) = when (i) {
                0 -> {
                    // Hook
                    val v = brief.hook
                    Quad("THE PROBLEM", brief.coreBenefit, v, null)
                }
                1 -> {
                    // Introduction / Claim 1
                    val c = claims.getOrNull(0)
                    val v = "Introducing ${brief.title}. Built from the ground up for modern engineering."
                    Quad(brief.title.uppercase(), "Engineered for Speed", v, c)
                }
                2 -> {
                    // Core Feature / Claim 2
                    val c = claims.getOrNull(1)
                    val claimText = c?.claimText ?: "Automated Code-to-Video Engine"
                    val v = "$claimText. Everything stays in your code."
                    Quad("CORE ENGINE", claimText, v, c)
                }
                3 -> {
                    // Architecture / Verification
                    val c = claims.getOrNull(2)
                    val claimText = c?.claimText ?: "Zero external cloud dependencies required"
                    val v = "$claimText. Clean, deterministic, and verifiable."
                    Quad("ARCHITECTURE", "Deterministic & Open", v, c)
                }
                sceneCount - 1 -> {
                    // Outro / Call to Action
                    val v = "${brief.callToAction}. Ready in seconds."
                    Quad("GET STARTED", brief.callToAction, v, null)
                }
                else -> {
                    // Unverified or secondary claim
                    val unverified = ClaimEvidence(
                        claimText = "Designed to 10x developer reach on social platforms",
                        sourceFile = "brief/marketing.md",
                        lineReference = "estimated",
                        evidenceSnippet = "Projected productivity metric",
                        isVerified = false,
                        verificationNotes = "Unverified marketing projection. Confirm before ad spend."
                    )
                    val v = "Amplify your open-source projects and engage contributors instantly."
                    Quad("ENGAGEMENT", "Reach More Developers", v, unverified)
                }
            }

            // Enforce max characters per voiceover
            val clampedVoiceover = voiceover.take(MAX_VOICEOVER_CHARS_PER_SCENE)
            val isVerified = claim?.isVerified ?: true

            scriptLines.add(
                ScriptLine(
                    sceneIndex = i,
                    title = title,
                    subtitle = subtitle,
                    voiceover = clampedVoiceover,
                    linkedClaim = claim,
                    isVerifiedClaim = isVerified,
                    characterCount = clampedVoiceover.length
                )
            )
        }

        return scriptLines
    }

    private data class Quad(
        val first: String,
        val second: String,
        val third: String,
        val fourth: ClaimEvidence?
    )
}
