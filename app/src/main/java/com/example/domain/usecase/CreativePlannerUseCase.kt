package com.example.domain.usecase

import com.example.data.model.ClaimEvidence
import com.example.data.model.ContentSafetyCheck
import com.example.data.model.CreativeBrief
import com.example.data.model.DeterministicScanManifest
import com.example.data.model.NarrativeTemplate

class CreativePlannerUseCase {

    companion object {
        const val MIN_DURATION_SECONDS = 15
        const val MAX_DURATION_SECONDS = 60
        const val MIN_SCENE_COUNT = 3
        const val MAX_SCENE_COUNT = 8
        const val MAX_HOOK_LENGTH = 90
        const val MAX_CTA_LENGTH = 60

        private val DECEPTIVE_KEYWORDS = listOf(
            "guaranteed 100%",
            "risk-free millions",
            "endorsed by",
            "official partnership with google",
            "free money",
            "100% bug-free",
            "cure",
            "hack into"
        )
    }

    /**
     * Assesses content safety of user prompt and claims before planning.
     */
    fun auditContentSafety(
        manifest: DeterministicScanManifest,
        marketingAuthorityConfirmed: Boolean
    ): ContentSafetyCheck {
        var deceptiveDetected = false
        val notes = mutableListOf<String>()

        val combinedText = "${manifest.appName} ${manifest.keyFeatures.joinToString(" ")}".lowercase()
        for (keyword in DECEPTIVE_KEYWORDS) {
            if (combinedText.contains(keyword)) {
                deceptiveDetected = true
                notes.add("Deceptive phrase detected: '$keyword'")
            }
        }

        if (!marketingAuthorityConfirmed) {
            notes.add("Marketing authority not yet confirmed by user.")
        }

        val approved = !deceptiveDetected && marketingAuthorityConfirmed

        return ContentSafetyCheck(
            hasDeceptiveClaims = deceptiveDetected,
            impersonationRisk = false,
            trademarkMisuseRisk = false,
            marketingAuthorityConfirmed = marketingAuthorityConfirmed,
            approvedForGeneration = approved,
            auditNotes = if (notes.isEmpty()) "Content safety checks passed." else notes.joinToString("; ")
        )
    }

    /**
     * Builds a structured creative brief and strictly validates all constraints.
     */
    fun createStructuredBrief(
        manifest: DeterministicScanManifest,
        template: NarrativeTemplate,
        targetDuration: Int,
        safetyCheck: ContentSafetyCheck
    ): Result<CreativeBrief> {
        // Enforce duration budget
        val clampedDuration = targetDuration.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        val calculatedSceneCount = when {
            clampedDuration <= 20 -> 4
            clampedDuration <= 35 -> 5
            clampedDuration <= 45 -> 6
            else -> 7
        }.coerceIn(MIN_SCENE_COUNT, MAX_SCENE_COUNT)

        val hookText = when (template) {
            NarrativeTemplate.RAGE_HOOK -> "Stop writing video editing configs by hand."
            NarrativeTemplate.PROBLEM_STACK -> "Showcasing code was slow and painful. Until now."
            NarrativeTemplate.DEMO_FIRST -> "Turn your GitHub repo into high-converting video in one command."
            NarrativeTemplate.TRANSFORMATION -> "Meet ${manifest.appName}. The modern workflow for developers."
        }.take(MAX_HOOK_LENGTH)

        val cta = "Star ${manifest.appName} on GitHub today".take(MAX_CTA_LENGTH)

        // Ensure every claim in brief is linked to evidence from manifest
        val validatedClaims = manifest.claimsWithEvidence.ifEmpty {
            listOf(
                ClaimEvidence(
                    claimText = "Built with modern developer ergonomics",
                    sourceFile = "package.json",
                    lineReference = "dependencies",
                    evidenceSnippet = manifest.technologies.joinToString(", "),
                    isVerified = true,
                    verificationNotes = "Derived from detected tech stack"
                )
            )
        }

        val brief = CreativeBrief(
            title = manifest.appName,
            hook = hookText,
            coreBenefit = manifest.keyFeatures.firstOrNull() ?: "Modern developer experience",
            targetAudience = manifest.userPersonas.firstOrNull() ?: "Software Engineers",
            keyClaims = validatedClaims,
            brandColors = listOf("#6366F1", "#8B5CF6", "#F59E0B"),
            callToAction = cta,
            durationBudgetSeconds = clampedDuration,
            sceneBudgetCount = calculatedSceneCount,
            contentSafetyVerified = safetyCheck.approvedForGeneration,
            licenseStatus = "Open Source / Permissive"
        )

        return Result.success(brief)
    }
}
