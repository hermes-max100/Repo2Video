package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Repository Ingestion Consent & Privacy Configuration
 */
data class IngestionConsentPolicy(
    val repoUrlOrPath: String,
    val readReadmeOnly: Boolean = true,
    val readPackageMetadata: Boolean = true,
    val readArchitectureSummary: Boolean = true,
    val excludeDotEnv: Boolean = true,
    val excludeCredentials: Boolean = true,
    val excludeKeystores: Boolean = true,
    val excludeLockfiles: Boolean = true,
    val excludeBuildArtifacts: Boolean = true,
    val excludeVendorDirs: Boolean = true,
    val customExclusions: List<String> = emptyList(),
    val sendFullSourceToAi: Boolean = false, // Deny by default: only send sanitized manifest
    val allowFirebaseCloudSync: Boolean = true,
    val contentAuthorityConfirmed: Boolean = false,
    val agreedAt: Long = System.currentTimeMillis()
)

/**
 * Result of Deny-By-Default Sensitive File & Secret Filtering
 */
data class SanitizedPayload(
    val rawCharacterCount: Int,
    val sanitizedCharacterCount: Int,
    val blockedSecretCount: Int,
    val excludedFileCount: Int,
    val detectedSensitivePatterns: List<String>,
    val sanitizedContentPreview: String,
    val isCleanToTransmit: Boolean
)

/**
 * Claim Evidence linking generated script statements to source proof
 */
data class ClaimEvidence(
    val id: String = UUID.randomUUID().toString(),
    val claimText: String,
    val sourceFile: String,
    val lineReference: String,
    val evidenceSnippet: String,
    val isVerified: Boolean = true,
    val verificationNotes: String = "Verified in repository source"
)

/**
 * Deterministic Scan Manifest: compact, editable source-of-truth
 */
data class DeterministicScanManifest(
    val appName: String,
    val rawRepoUrl: String,
    val normalizedRepoName: String,
    val technologies: List<String>,
    val keyFeatures: List<String>,
    val userPersonas: List<String>,
    val notableFiles: List<String>,
    val claimsWithEvidence: List<ClaimEvidence>,
    val excludedFiles: List<String>,
    val rawSourceDeleted: Boolean = true,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Structured Creative Brief with strict schema bounds
 */
data class CreativeBrief(
    val title: String,
    val hook: String,
    val coreBenefit: String,
    val targetAudience: String,
    val keyClaims: List<ClaimEvidence>,
    val brandColors: List<String>,
    val callToAction: String,
    val durationBudgetSeconds: Int = 30,
    val sceneBudgetCount: Int = 5,
    val contentSafetyVerified: Boolean = true,
    val licenseStatus: String = "Commercial Use Ready"
)

/**
 * Licensing & Attribution Metadata for assets
 */
data class LicensingAttribution(
    val assetName: String,
    val assetType: String, // "CODE_FONT", "SYNTH_VOICE", "AUDIO_BED", "MOTION_CANVAS"
    val source: String,
    val licenseType: String, // "MIT", "APACHE_2.0", "CC0", "ELEVENLABS_COMMERCIAL"
    val publishableStatus: String = "SAFE_TO_PUBLISH",
    val attributionNotice: String
)

/**
 * Content Safety Assessment
 */
data class ContentSafetyCheck(
    val hasDeceptiveClaims: Boolean = false,
    val impersonationRisk: Boolean = false,
    val trademarkMisuseRisk: Boolean = false,
    val marketingAuthorityConfirmed: Boolean = true,
    val approvedForGeneration: Boolean = true,
    val auditNotes: String = "Clean repository metadata. No unauthorized trademarks detected."
)

/**
 * Render Job Status & Types for Durable Background Execution
 */
enum class RenderJobType {
    VOICEOVER_SYNTHESIS,
    VIDEO_FRAME_RENDER,
    REMOTION_BUNDLE_EXPORT,
    CLOUD_BACKUP_SYNC
}

enum class RenderJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Durable Render Job Entity for Room Persistence
 */
@Entity(tableName = "render_jobs")
data class RenderJobEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: Long,
    val jobType: String = RenderJobType.VIDEO_FRAME_RENDER.name,
    val status: String = RenderJobStatus.QUEUED.name,
    val progressPercent: Int = 0,
    val checkpointStep: String = "INITIALIZED",
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val isResumable: Boolean = true,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val retryCount: Int = 0
)

/**
 * Sync State tracking for Room Local Database as Single Source of Truth
 */
enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    SYNC_PENDING,
    CONFLICT,
    FAILED
}

/**
 * Shareable Project Bundle format (.devdirector.json)
 */
data class ProjectBundle(
    val formatVersion: String = "1.0",
    val projectId: Long,
    val projectTitle: String,
    val brandProfile: BrandProfile,
    val scanManifest: DeterministicScanManifest,
    val creativeBrief: CreativeBrief,
    val scenes: List<PromoScene>,
    val claims: List<ClaimEvidence>,
    val licensingManifest: List<LicensingAttribution>,
    val voiceConfig: String,
    val exportedAt: Long = System.currentTimeMillis()
)
