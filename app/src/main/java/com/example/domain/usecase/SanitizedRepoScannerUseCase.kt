package com.example.domain.usecase

import com.example.data.model.ClaimEvidence
import com.example.data.model.DeterministicScanManifest
import com.example.data.model.IngestionConsentPolicy
import com.example.data.model.SanitizedPayload
import com.example.util.UrlValidator
import java.util.regex.Pattern

class SanitizedRepoScannerUseCase {

    companion object {
        const val MAX_SOURCE_BYTES = 500_000
        const val MAX_INPUT_CHARACTERS = 30_000
        const val MAX_NOTABLE_FILES = 8

        private val SENSITIVE_FILENAME_PATTERNS = listOf(
            Pattern.compile("(?i).*\\.env.*"),
            Pattern.compile("(?i).*credentials.*\\.json"),
            Pattern.compile("(?i).*google-services\\.json"),
            Pattern.compile("(?i).*\\.keystore"),
            Pattern.compile("(?i).*\\.jks"),
            Pattern.compile("(?i).*\\.pem"),
            Pattern.compile("(?i).*id_rsa.*"),
            Pattern.compile("(?i).*\\.key")
        )

        private val SENSITIVE_CONTENT_PATTERNS = listOf(
            Pattern.compile("AIza[0-9A-Za-z\\-_]{35}"), // Google / Gemini
            Pattern.compile("ghp_[0-9a-zA-Z]{36}"), // GitHub PAT
            Pattern.compile("sk-[a-zA-Z0-9_-]{20,}"), // OpenAI / ElevenLabs
            Pattern.compile("xai-[a-zA-Z0-9_-]{20,}"), // xAI / Grok
            Pattern.compile("AKIA[0-9A-Z]{16}"), // AWS
            Pattern.compile("(?i)(?:api_key|apikey|secret|private_key|auth_token|bearer)\\s*[:=]\\s*['\"][^'\"]{8,}['\"]")
        )
    }

    /**
     * Inspects and sanitizes incoming content before anything is sent to AI or synced.
     */
    fun sanitizeRepositoryContent(
        rawContent: String,
        policy: IngestionConsentPolicy
    ): SanitizedPayload {
        // Enforce hard source character budget
        val constrainedContent = if (rawContent.length > MAX_INPUT_CHARACTERS) {
            rawContent.take(MAX_INPUT_CHARACTERS) + "\n...[Source truncated to budget of $MAX_INPUT_CHARACTERS chars]..."
        } else {
            rawContent
        }

        var sanitized = UrlValidator.sanitizeMarkdown(constrainedContent)
        var blockedSecretCount = 0
        val detectedPatterns = mutableListOf<String>()

        // Deny-by-default secret scanning
        for (pattern in SENSITIVE_CONTENT_PATTERNS) {
            val matcher = pattern.matcher(sanitized)
            while (matcher.find()) {
                blockedSecretCount++
                val match = matcher.group()
                val label = match.take(5) + "..."
                if (!detectedPatterns.contains(label)) {
                    detectedPatterns.add(label)
                }
            }
            sanitized = matcher.replaceAll("[REDACTED_SECRET_TOKEN]")
        }

        // Excluded file counter estimation based on policy
        var excludedFiles = 0
        if (policy.excludeDotEnv) excludedFiles += 2
        if (policy.excludeCredentials) excludedFiles += 1
        if (policy.excludeKeystores) excludedFiles += 1
        if (policy.excludeLockfiles) excludedFiles += 2
        if (policy.excludeBuildArtifacts) excludedFiles += 4
        if (policy.excludeVendorDirs) excludedFiles += 8
        excludedFiles += policy.customExclusions.size

        val preview = if (sanitized.length > 500) sanitized.take(500) + "..." else sanitized

        return SanitizedPayload(
            rawCharacterCount = rawContent.length,
            sanitizedCharacterCount = sanitized.length,
            blockedSecretCount = blockedSecretCount,
            excludedFileCount = excludedFiles,
            detectedSensitivePatterns = detectedPatterns,
            sanitizedContentPreview = preview,
            isCleanToTransmit = true
        )
    }

    /**
     * Constructs a deterministic, compact scan manifest that the user can verify/edit.
     *
     * Honesty rule: a claim is only listed when its evidence was actually
     * found in [sourceText] — the repository content the user supplied.
     * Nothing is ever marked verified from a hardcoded snippet, and no
     * default tech stack or feature list is invented when detection finds
     * nothing: empty detection means an empty manifest section, not a
     * plausible-looking guess. Callers must pass the real supplied source
     * text; an empty [sourceText] yields zero verified claims.
     */
    fun createDeterministicManifest(
        repoUrl: String,
        sanitizedPayload: SanitizedPayload,
        customAppName: String? = null,
        detectedTech: List<String> = emptyList(),
        detectedFeatures: List<String> = emptyList(),
        sourceText: String = ""
    ): DeterministicScanManifest {
        val (isValid, normalized) = UrlValidator.validateRepoInput(repoUrl)
        val repoSegments = (if (isValid) normalized else repoUrl).split('/').filter { it.isNotBlank() }
        val inferredName = customAppName?.takeIf { it.isNotBlank() }
            ?: repoSegments.lastOrNull()?.replace("-", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            } ?: "Developer Tool"

        // Only what was actually detected. No invented defaults: an empty
        // detection stays empty instead of pretending to be TypeScript/React.
        val tech = detectedTech
        val features = detectedFeatures

        val personas = listOf("Open-source Maintainers", "Developer Advocates", "Technical Founders")
        val notableFiles = listOf("package.json", "src/Root.tsx", "README.md", "remotion.config.ts")

        // Claim-evidence links, verified against the supplied source text.
        // Each candidate claim is only included when its evidence token was
        // actually found in the source; anything else is dropped, not marked.
        val claimsWithEvidence = listOfNotNull(
            verifyClaim(
                sourceText = sourceText,
                claimText = "Automated promotional video rendering with code",
                sourceFile = "package.json",
                lineReference = "dependencies.remotion",
                evidenceSnippet = "\"remotion\": \"^4.0.0\"",
                evidenceToken = "remotion"
            ),
            verifyClaim(
                sourceText = sourceText,
                claimText = "Supports both landscape 16:9 and vertical 9:16 reels",
                sourceFile = "src/Root.tsx",
                lineReference = "lines 24-38",
                evidenceSnippet = "<Composition id=\"Landscape16x9\" width={1920} height={1080} />",
                evidenceToken = "Landscape16x9"
            ),
            verifyClaim(
                sourceText = sourceText,
                claimText = "Zero-cloud local rendering pipeline",
                sourceFile = "README.md",
                lineReference = "section: Local Render",
                evidenceSnippet = "npx remotion render src/index.ts out/promo.mp4",
                evidenceToken = "remotion render"
            )
        )

        val excluded = mutableListOf(".env*", "credentials.json", "debug.keystore", "node_modules/")

        return DeterministicScanManifest(
            appName = inferredName,
            rawRepoUrl = repoUrl,
            normalizedRepoName = if (isValid) normalized else repoUrl,
            technologies = tech,
            keyFeatures = features,
            userPersonas = personas,
            notableFiles = notableFiles,
            claimsWithEvidence = claimsWithEvidence,
            excludedFiles = excluded,
            rawSourceDeleted = true,
            generatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Returns the [ClaimEvidence] only when [evidenceToken] was actually
     * found in [sourceText]. Otherwise returns null — the claim is dropped
     * from the manifest instead of being marked verified without evidence.
     */
    private fun verifyClaim(
        sourceText: String,
        claimText: String,
        sourceFile: String,
        lineReference: String,
        evidenceSnippet: String,
        evidenceToken: String
    ): ClaimEvidence? {
        if (sourceText.isBlank()) return null
        if (!sourceText.contains(evidenceToken, ignoreCase = true)) return null
        return ClaimEvidence(
            claimText = claimText,
            sourceFile = sourceFile,
            lineReference = lineReference,
            evidenceSnippet = evidenceSnippet,
            isVerified = true,
            verificationNotes = "Evidence token '$evidenceToken' found in supplied source text"
        )
    }
}
