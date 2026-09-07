package com.example.data.model

import java.util.UUID

/**
 * Autonomous Creative Studio v2 Models
 */

/**
 * Verification Status in the Hard Claims Gate
 */
enum class ClaimGateStatus {
    VERIFIED_PASSED,
    SUBSTITUTED_WITH_FACT,
    BLOCKED_UNSUPPORTED
}

/**
 * Individual entry in the Evidence Ledger linking script claims to hard proof
 */
data class EvidenceLedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val claimText: String,
    val sourceFile: String,
    val lineReference: String,
    val evidenceSnippet: String,
    val status: ClaimGateStatus = ClaimGateStatus.VERIFIED_PASSED,
    val confidenceScore: Float = 0.98f,
    val auditNotes: String = "Hard gate verified against source AST & manifest",
    val verifiedAt: Long = System.currentTimeMillis()
)

/**
 * Standalone Evidence Ledger File model
 */
data class EvidenceLedger(
    val projectName: String,
    val totalClaimsAudited: Int,
    val verifiedClaimsCount: Int,
    val blockedClaimsCount: Int,
    val substitutedClaimsCount: Int,
    val entries: List<EvidenceLedgerEntry>,
    val auditSummary: String,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Audit record of a claim filtered or modified by the Hard Claims Gate
 */
data class AuditedClaimAction(
    val originalClaim: String,
    val action: ClaimGateStatus,
    val resultingCopy: String,
    val sourceEvidenceRef: String,
    val rationale: String
)

/**
 * Result returned by the Hard Claims Gate
 */
data class HardClaimsGateResult(
    val isApproved: Boolean,
    val totalAudited: Int,
    val passedCount: Int,
    val blockedCount: Int,
    val actions: List<AuditedClaimAction>,
    val sanitizedScriptCopy: List<String>,
    val ledger: EvidenceLedger
)

/**
 * Creative Director Stage: Audience, Angle, Tone, and CTA decisions
 */
data class CreativeDirectorPillars(
    val audience: String,
    val angle: String,
    val tone: String,
    val cta: String,
    val directorRationale: String,
    val selectedAt: Long = System.currentTimeMillis()
)

/**
 * Distinct hook variants generated for testing
 */
data class HookVariant(
    val id: String, // HOOK_A, HOOK_B, HOOK_C
    val label: String,
    val hookText: String,
    val psychologicalAngle: String,
    val predictedRetentionPercent: Int,
    val characterCount: Int,
    val verifiedEvidenceId: String? = null
)

/**
 * Platform-Aware Layout Profile (NOT just cropping)
 */
data class PlatformLayoutProfile(
    val id: String,
    val name: String,
    val aspectRatio: AspectRatioFormat,
    val targetPlatform: String,
    val splitLayoutType: String, // DUAL_PANE_CODE_DIFF, VERTICAL_KINETIC_STACK, CENTER_WEIGHTED_EXECUTIVE
    val safeZonePaddingTopDp: Int,
    val safeZonePaddingBottomDp: Int,
    val fontScaleMultiplier: Float,
    val description: String
)

/**
 * Multi-factor scorecard for evaluating creative variants
 */
data class VariantScoreCard(
    val hookRetentionScore: Int, // 0-100
    val visualDensityScore: Int, // 0-100
    val claimVerifiabilityScore: Int, // 0-100
    val pacingBalanceScore: Int, // 0-100
    val compositeScore: Int, // 0-100
    val winnerJustification: String
)

/**
 * Full A / B / C Video Variant
 */
data class CreativeVariant(
    val id: String, // "A", "B", "C"
    val label: String,
    val hook: HookVariant,
    val layoutProfile: PlatformLayoutProfile,
    val scenes: List<PromoScene>,
    val scoreCard: VariantScoreCard,
    val isRecommendedWinner: Boolean = false
)

/**
 * Keyframe extracted post-render for automated visual QA
 */
data class ExtractedKeyframe(
    val keyframeId: String = UUID.randomUUID().toString(),
    val sceneIndex: Int,
    val timestampSeconds: Float,
    val frameLabel: String,
    val textContrastRatio: Float, // e.g. 5.8:1 (WCAG AA is 4.5:1)
    val safeZoneCompliance: Boolean,
    val pacingWordsPerSecond: Float,
    val isRealProductCapture: Boolean,
    val visualDescription: String,
    val needsRepair: Boolean = false
)

/**
 * Automated Visual QA Critique Action (Repair Loop)
 */
data class VisualQARepairAction(
    val targetSceneIndex: Int,
    val timestampSeconds: Float,
    val defectDetected: String,
    val autoRepairApplied: String,
    val postRepairQualityDelta: String
)

/**
 * Complete Visual QA Critique and Repair Report
 */
data class VisualQACritiqueReport(
    val totalKeyframesInspected: Int,
    val contrastPassed: Boolean,
    val safeZonePassed: Boolean,
    val pacingPassed: Boolean,
    val realCapturePreferred: Boolean,
    val overallQAScore: Int, // 0-100
    val repairsApplied: List<VisualQARepairAction>,
    val keyframes: List<ExtractedKeyframe>,
    val contactSheetSummary: String
)

/**
 * Full Autonomous Creative Studio Delivery Package
 */
data class AutonomousDeliveryPackage(
    val creativeBriefJson: String,
    val evidenceLedgerJson: String,
    val variantReportJson: String,
    val qaReportWithContactSheetJson: String,
    val deliveryReportJson: String,
    val finalVideosSummary: String,
    val recommendedWinnerId: String
) {
    val fullPackageJson: String
        get() = org.json.JSONObject().apply {
            put("creative_brief", creativeBriefJson)
            put("evidence_ledger", evidenceLedgerJson)
            put("variant_report", variantReportJson)
            put("qa_critique_report_with_contact_sheet", qaReportWithContactSheetJson)
            put("delivery_report", deliveryReportJson)
            put("final_videos_summary", finalVideosSummary)
            put("recommended_winner", recommendedWinnerId)
        }.toString(2)
}
