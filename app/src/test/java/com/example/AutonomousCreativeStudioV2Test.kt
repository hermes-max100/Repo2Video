package com.example

import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.ClaimEvidence
import com.example.data.model.ClaimGateStatus
import com.example.data.model.CreativeBrief
import com.example.data.model.DeterministicScanManifest
import com.example.domain.usecase.AutonomousCreativeDirectorEngine
import com.example.domain.usecase.HardClaimsGateUseCase
import com.example.service.AudioSynthEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Local JVM tests verifying the Autonomous Creative Studio v2 upgrade requirements:
 * 1. Evidence ledger & hard claims gate preventing unsupported claims.
 * 2. Real product capture preference over fabricated UI.
 * 3. Creative Director stage picking audience, angle, tone, and CTA.
 * 4. At least three distinct hook variants generated.
 * 5. Platform-aware layout profiles (not just crops).
 * 6. Audio fallback safety bed preventing silence when voice fails.
 * 7. Post-render keyframe extraction and automatic visual QA repair loop.
 * 8. Variant scoring & recommended winner selection.
 * 9. Full package export (brief, ledger, variant report, QA report with contact sheet, delivery report, final videos).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutonomousCreativeStudioV2Test {

    private val hardClaimsGate = HardClaimsGateUseCase()
    private val creativeDirector = AutonomousCreativeDirectorEngine(hardClaimsGate)

    private val testManifest = DeterministicScanManifest(
        appName = "DevDirector",
        rawRepoUrl = "https://github.com/example/devdirector",
        normalizedRepoName = "devdirector",
        technologies = listOf("Kotlin", "Jetpack Compose", "Gradle", "Node.js"),
        keyFeatures = listOf(
            "CLI-first workflow",
            "Zero hallucination claims gate",
            "Automated Remotion scene compilation",
            "Platform aware video mastering"
        ),
        userPersonas = listOf("Software Engineers", "DevOps Architects"),
        notableFiles = listOf("MainActivity.kt", "PromoViewModel.kt"),
        claimsWithEvidence = listOf(
            ClaimEvidence(
                claimText = "CLI-first workflow",
                sourceFile = "package.json",
                lineReference = "bin/cli",
                evidenceSnippet = "\"bin\": \"cli.js\"",
                isVerified = true
            )
        ),
        excludedFiles = listOf(".env", "debug.keystore")
    )

    private val testBrand = BrandProfile(
        name = "DevDirector",
        tagline = "Autonomous Promo Video Studio",
        description = "Deterministic code-to-video studio",
        primaryColorHex = "#6366F1",
        secondaryColorHex = "#8B5CF6",
        accentColorHex = "#06B6D4"
    )

    @Test
    fun testHardClaimsGate_filtersUnsupportedClaimsAndCreatesLedger() {
        val ledger = hardClaimsGate.buildEvidenceLedger(testManifest)

        // Ledger must exist and contain verified entries
        assertNotNull(ledger)
        assertTrue(ledger.entries.isNotEmpty())
        assertEquals("DevDirector", ledger.projectName)

        val rawClaims = listOf(
            "DevDirector is guaranteed 10x faster than all tools", // Speculative / unsupported pattern
            "Powered by Kotlin, Jetpack Compose, Gradle", // Grounded
            "World's best promotional studio with zero bugs", // Speculative / unsupported pattern
            "CLI-first workflow verified in repository source" // Grounded
        )

        val result = hardClaimsGate.auditAndGateScript(
            proposedLines = rawClaims,
            ledger = ledger
        )

        // Unsupported claims must be audited and substituted with verified ground-truth facts
        assertEquals(2, result.blockedCount)
        assertEquals(2, result.passedCount)
        assertTrue(result.actions.any { it.action == ClaimGateStatus.SUBSTITUTED_WITH_FACT })

        // Sanitized copy must not contain the wild ungrounded claims
        result.sanitizedScriptCopy.forEach { copy ->
            assertFalse(copy.contains("guaranteed 10x faster"))
            assertFalse(copy.contains("World's best promotional studio"))
        }
    }

    @Test
    fun testCreativeDirector_decidesPillarsAndGeneratesThreeHooks() {
        val pillars = creativeDirector.decideCreativePillars(testBrand, testManifest)

        assertNotNull(pillars.audience)
        assertNotNull(pillars.angle)
        assertNotNull(pillars.tone)
        assertNotNull(pillars.cta)
        assertTrue(pillars.audience.isNotBlank())
        assertTrue(pillars.cta.isNotBlank())

        val ledger = hardClaimsGate.buildEvidenceLedger(testManifest)
        val hooks = creativeDirector.generateHookVariants(testBrand, pillars, ledger)
        assertTrue("Must generate at least three hook variants", hooks.size >= 3)

        val hookIds = hooks.map { it.id }
        assertTrue(hookIds.contains("HOOK_A"))
        assertTrue(hookIds.contains("HOOK_B"))
        assertTrue(hookIds.contains("HOOK_C"))
    }

    @Test
    fun testPlatformAwareVariants_rendersVariantsAndRealProductCapture() {
        val pillars = creativeDirector.decideCreativePillars(testBrand, testManifest)
        val ledger = hardClaimsGate.buildEvidenceLedger(testManifest)
        val hooks = creativeDirector.generateHookVariants(testBrand, pillars, ledger)
        val layouts = creativeDirector.getPlatformLayoutProfiles()

        val brief = CreativeBrief(
            title = testBrand.name,
            hook = hooks[0].hookText,
            coreBenefit = pillars.angle,
            targetAudience = pillars.audience,
            keyClaims = testManifest.claimsWithEvidence,
            brandColors = listOf(testBrand.primaryColorHex, testBrand.secondaryColorHex),
            callToAction = pillars.cta,
            durationBudgetSeconds = 30
        )

        val variants = creativeDirector.composeVariants(
            brand = testBrand,
            brief = brief,
            hooks = hooks,
            layouts = layouts,
            ledger = ledger
        )

        // 3 Platform-aware variants (A, B, C)
        assertEquals(3, variants.size)
        assertEquals(AspectRatioFormat.LANDSCAPE_16_9, variants[0].layoutProfile.aspectRatio)
        assertEquals(AspectRatioFormat.PORTRAIT_9_16, variants[1].layoutProfile.aspectRatio)

        // Winner must be picked among variants
        assertTrue(variants.any { it.isRecommendedWinner })

        // Real product capture must be strictly prioritized
        variants.forEach { variant ->
            assertTrue(variant.scenes.any { it.isRealProductCapture })
        }
    }

    @Test
    fun testVisualQA_extractsKeyframesAndExecutesRepairLoop() {
        val pillars = creativeDirector.decideCreativePillars(testBrand, testManifest)
        val ledger = hardClaimsGate.buildEvidenceLedger(testManifest)
        val hooks = creativeDirector.generateHookVariants(testBrand, pillars, ledger)
        val layouts = creativeDirector.getPlatformLayoutProfiles()

        val brief = CreativeBrief(
            title = testBrand.name,
            hook = hooks[0].hookText,
            coreBenefit = pillars.angle,
            targetAudience = pillars.audience,
            keyClaims = testManifest.claimsWithEvidence,
            brandColors = listOf(testBrand.primaryColorHex, testBrand.secondaryColorHex),
            callToAction = pillars.cta,
            durationBudgetSeconds = 30
        )

        val variants = creativeDirector.composeVariants(testBrand, brief, hooks, layouts, ledger)
        val targetVariant = variants[1] // 9:16 Portrait variant

        val (repairedVariant, qaReport) = creativeDirector.runVisualQAAndRepairLoop(targetVariant, ledger)

        assertTrue(qaReport.totalKeyframesInspected >= 4)
        assertTrue(qaReport.keyframes.isNotEmpty())
        assertTrue(qaReport.repairsApplied.isNotEmpty())
        assertTrue(qaReport.overallQAScore in 80..100)
        assertNotNull(repairedVariant)
    }

    @Test
    fun testFullPackageExport_containsAllRequiredArtifacts() {
        val pillars = creativeDirector.decideCreativePillars(testBrand, testManifest)
        val ledger = hardClaimsGate.buildEvidenceLedger(testManifest)
        val hooks = creativeDirector.generateHookVariants(testBrand, pillars, ledger)
        val layouts = creativeDirector.getPlatformLayoutProfiles()

        val brief = CreativeBrief(
            title = testBrand.name,
            hook = hooks[0].hookText,
            coreBenefit = pillars.angle,
            targetAudience = pillars.audience,
            keyClaims = testManifest.claimsWithEvidence,
            brandColors = listOf(testBrand.primaryColorHex, testBrand.secondaryColorHex),
            callToAction = pillars.cta,
            durationBudgetSeconds = 30
        )

        val variants = creativeDirector.composeVariants(testBrand, brief, hooks, layouts, ledger)
        val (repairedWinner, qaReport) = creativeDirector.runVisualQAAndRepairLoop(variants[1], ledger)

        val pkg = creativeDirector.buildDeliveryPackage(
            brief = brief,
            ledger = ledger,
            variants = variants,
            qaReport = qaReport,
            winnerId = repairedWinner.id
        )

        assertTrue(pkg.creativeBriefJson.contains("targetAudience"))
        assertTrue(pkg.evidenceLedgerJson.contains("verifiedClaimsCount"))
        assertTrue(pkg.variantReportJson.contains("variants"))
        assertTrue(pkg.qaReportWithContactSheetJson.contains("keyframes"))
        assertTrue(pkg.deliveryReportJson.contains("master_artifacts"))
        assertTrue(pkg.finalVideosSummary.contains("Landscape 16:9"))
        assertTrue(pkg.finalVideosSummary.contains("Portrait 9:16"))
        assertTrue(pkg.fullPackageJson.isNotBlank())
    }

    @Test
    fun testAudioSafetyBed_ensuresNoSilenceOnVoiceoverFail() {
        val audioEngine = AudioSynthEngine()
        audioEngine.ensureAudibleSafetyBed()
        assertTrue(audioEngine.isAudioActive())
    }
}
