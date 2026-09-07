package com.example.domain.usecase

import com.example.data.local.PromoRepository
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.ClaimEvidence
import com.example.data.model.CreativeBrief
import com.example.data.model.DeterministicScanManifest
import com.example.data.model.EmotionalPreset
import com.example.data.model.LicensingAttribution
import com.example.data.model.MusicTrackOption
import com.example.data.model.NarrativeTemplate
import com.example.data.model.ProjectBundle
import com.example.data.model.PromoProject
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.data.model.TransitionStyle
import com.example.data.model.VoiceActor
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Robust Project Importer for DevDirector:
 * - Parses exported ProjectBundle JSON (.devdirector.json)
 * - Parses raw GitHub / npm / manifest JSON
 * - Validates schema, handles backwards compatibility, and generates complete hydrated projects.
 */
class ProjectBundleImporter {

    sealed class ImportResult {
        data class Success(val bundle: ProjectBundle, val project: PromoProject) : ImportResult()
        data class Error(val message: String, val exception: Throwable? = null) : ImportResult()
    }

    /**
     * Parses arbitrary JSON (either a full DevDirector ProjectBundle or repository metadata)
     */
    fun importFromJson(jsonString: String): ImportResult {
        if (jsonString.isBlank()) {
            return ImportResult.Error("Input JSON string is empty")
        }

        return try {
            val root = JSONObject(jsonString.trim())

            // Check if it's a full ProjectBundle
            if (root.has("projectTitle") || root.has("brandProfile") || root.has("scenes")) {
                parseFullBundle(root)
            } else if (root.has("name") && (root.has("dependencies") || root.has("description"))) {
                // Package.json / repo manifest format
                parsePackageJsonManifest(root)
            } else {
                // Generic JSON with title/scenes or fallback
                parseGenericProjectJson(root)
            }
        } catch (e: Exception) {
            ImportResult.Error("Invalid JSON structure: ${e.localizedMessage ?: "Unknown parse error"}", e)
        }
    }

    private fun parseFullBundle(root: JSONObject): ImportResult {
        val projectId = root.optLong("projectId", System.currentTimeMillis())
        val projectTitle = root.optString("projectTitle", root.optString("title", "Imported Project"))
        val voiceConfig = root.optString("voiceConfig", "ElevenLabs HD Neural")
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

        // 1. Brand Profile
        val brandObj = root.optJSONObject("brandProfile") ?: JSONObject()
        val brandProfile = BrandProfile(
            name = brandObj.optString("name", projectTitle),
            tagline = brandObj.optString("tagline", "Automated code-to-video showcase"),
            description = brandObj.optString("description", "Showcase your product with DevDirector"),
            primaryColorHex = brandObj.optString("primaryColorHex", "#6366F1"),
            secondaryColorHex = brandObj.optString("secondaryColorHex", "#8B5CF6"),
            accentColorHex = brandObj.optString("accentColorHex", "#06B6D4"),
            targetAudience = brandObj.optString("targetAudience", "Developers and Tech Enthusiasts"),
            repoPathOrUrl = brandObj.optString("repoUrl", "https://github.com/project/demo")
        )

        // 2. Scan Manifest
        val manifestObj = root.optJSONObject("scanManifest") ?: JSONObject()
        val technologies = jsonArrayToStringList(manifestObj.optJSONArray("technologies")).ifEmpty {
            listOf("Kotlin", "Jetpack Compose", "Remotion", "TypeScript")
        }
        val keyFeatures = jsonArrayToStringList(manifestObj.optJSONArray("keyFeatures")).ifEmpty {
            listOf("Deterministic Video Generation", "Voiceover Integration", "Multi-Format Export")
        }
        val userPersonas = jsonArrayToStringList(manifestObj.optJSONArray("userPersonas")).ifEmpty {
            listOf("Full-stack Developers", "Open Source Authors", "Product Managers")
        }
        val notableFiles = jsonArrayToStringList(manifestObj.optJSONArray("notableFiles"))
        val excludedFiles = jsonArrayToStringList(manifestObj.optJSONArray("excludedFiles"))

        val scanManifest = DeterministicScanManifest(
            appName = manifestObj.optString("appName", brandProfile.name),
            rawRepoUrl = manifestObj.optString("rawRepoUrl", brandProfile.repoPathOrUrl),
            normalizedRepoName = manifestObj.optString("normalizedRepoName", brandProfile.name.lowercase().replace(" ", "-")),
            technologies = technologies,
            keyFeatures = keyFeatures,
            userPersonas = userPersonas,
            notableFiles = notableFiles,
            claimsWithEvidence = emptyList(),
            excludedFiles = excludedFiles,
            rawSourceDeleted = manifestObj.optBoolean("rawSourceDeleted", true),
            generatedAt = manifestObj.optLong("generatedAt", System.currentTimeMillis())
        )

        // 3. Creative Brief
        val briefObj = root.optJSONObject("creativeBrief") ?: JSONObject()
        val creativeBrief = CreativeBrief(
            title = briefObj.optString("title", projectTitle),
            hook = briefObj.optString("hook", "What if your code could film its own launch video in 60 seconds?"),
            coreBenefit = briefObj.optString("coreBenefit", "Turn git repositories into viral promo videos"),
            targetAudience = briefObj.optString("targetAudience", brandProfile.targetAudience),
            keyClaims = emptyList(),
            brandColors = listOf(brandProfile.primaryColorHex, brandProfile.secondaryColorHex, brandProfile.accentColorHex),
            callToAction = briefObj.optString("callToAction", "Star the repository on GitHub"),
            durationBudgetSeconds = briefObj.optInt("durationBudgetSeconds", 30),
            sceneBudgetCount = briefObj.optInt("sceneBudgetCount", 5),
            contentSafetyVerified = briefObj.optBoolean("contentSafetyVerified", true),
            licenseStatus = briefObj.optString("licenseStatus", "Commercial Use Ready")
        )

        // 4. Scenes
        val scenesArr = root.optJSONArray("scenes")
        val scenes = mutableListOf<PromoScene>()
        if (scenesArr != null && scenesArr.length() > 0) {
            for (i in 0 until scenesArr.length()) {
                val sObj = scenesArr.getJSONObject(i)
                val visualTypeStr = sObj.optString("visualType", SceneVisualType.CODE_TERMINAL.name)
                val visualType = try {
                    SceneVisualType.valueOf(visualTypeStr)
                } catch (_: Exception) {
                    SceneVisualType.CODE_TERMINAL
                }

                val emotionStr = sObj.optString("emotionalPreset", EmotionalPreset.CONFIDENT.name)
                val emotionalPreset = try {
                    EmotionalPreset.valueOf(emotionStr)
                } catch (_: Exception) {
                    EmotionalPreset.CONFIDENT
                }

                val badgesArr = sObj.optJSONArray("badgeTags")
                val badges = if (badgesArr != null) jsonArrayToStringList(badgesArr) else emptyList()

                scenes.add(
                    PromoScene(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        orderIndex = sObj.optInt("orderIndex", i),
                        title = sObj.optString("title", "Scene ${i + 1}"),
                        subtitle = sObj.optString("subtitle", ""),
                        voiceover = sObj.optString("voiceover", ""),
                        emotionalPreset = emotionalPreset,
                        durationSeconds = sObj.optDouble("durationSeconds", 4.0).toFloat(),
                        visualType = visualType,
                        accentTag = sObj.optString("accentTag", ""),
                        codeSnippet = sObj.optString("codeSnippet").ifEmpty { null },
                        metricNumber = sObj.optString("metricNumber").ifEmpty { null },
                        metricLabel = sObj.optString("metricLabel").ifEmpty { null },
                        badgeTags = badges,
                        ctaButtonText = sObj.optString("ctaButtonText").ifEmpty { null },
                        ctaUrl = sObj.optString("ctaUrl").ifEmpty { null }
                    )
                )
            }
        } else {
            // Generate fallback default scenes if array was empty
            scenes.addAll(generateFallbackScenes(projectTitle, brandProfile))
        }

        // 5. Claims & Licensing
        val claims = mutableListOf<ClaimEvidence>()
        val claimsArr = root.optJSONArray("claimEvidenceAuditTrail") ?: root.optJSONArray("claims")
        if (claimsArr != null) {
            for (i in 0 until claimsArr.length()) {
                val cObj = claimsArr.getJSONObject(i)
                claims.add(
                    ClaimEvidence(
                        id = cObj.optString("id", UUID.randomUUID().toString()),
                        claimText = cObj.optString("claimText", "Key verified capability"),
                        sourceFile = cObj.optString("sourceFile", "src/index.ts"),
                        lineReference = cObj.optString("lineReference", "line 1"),
                        evidenceSnippet = cObj.optString("evidenceSnippet", "Exported API"),
                        isVerified = cObj.optBoolean("isVerified", true),
                        verificationNotes = cObj.optString("verificationNotes", "Verified from codebase")
                    )
                )
            }
        }

        val licensingManifest = mutableListOf<LicensingAttribution>()
        val licenseArr = root.optJSONArray("licensingAttributions") ?: root.optJSONArray("licensingManifest")
        if (licenseArr != null) {
            for (i in 0 until licenseArr.length()) {
                val lObj = licenseArr.getJSONObject(i)
                licensingManifest.add(
                    LicensingAttribution(
                        assetName = lObj.optString("assetName", "Asset"),
                        assetType = lObj.optString("assetType", "MOTION_CANVAS"),
                        source = lObj.optString("source", "DevDirector"),
                        licenseType = lObj.optString("licenseType", "MIT"),
                        publishableStatus = lObj.optString("publishableStatus", "SAFE_TO_PUBLISH"),
                        attributionNotice = lObj.optString("attributionNotice", "Open Source Asset")
                    )
                )
            }
        }

        val bundle = ProjectBundle(
            formatVersion = root.optString("formatVersion", "1.0"),
            projectId = projectId,
            projectTitle = projectTitle,
            brandProfile = brandProfile,
            scanManifest = scanManifest,
            creativeBrief = creativeBrief,
            scenes = scenes,
            claims = claims,
            licensingManifest = licensingManifest,
            voiceConfig = voiceConfig,
            exportedAt = exportedAt
        )

        val project = PromoProject(
            id = projectId,
            title = projectTitle,
            tagline = brandProfile.tagline,
            repoUrl = brandProfile.repoPathOrUrl,
            brandName = brandProfile.name,
            primaryColorHex = brandProfile.primaryColorHex,
            secondaryColorHex = brandProfile.secondaryColorHex,
            accentColorHex = brandProfile.accentColorHex,
            techStackJson = JSONArray(technologies).toString(),
            featuresJson = JSONArray(keyFeatures).toString(),
            narrativeTemplate = NarrativeTemplate.PROBLEM_STACK.name,
            targetDurationSeconds = creativeBrief.durationBudgetSeconds,
            voiceActor = VoiceActor.RACHEL.name,
            musicTrack = MusicTrackOption.INSPIRED_AMBIENT.name,
            transitionStyle = TransitionStyle.METALLIC_SWOOSH.name,
            scenesJson = PromoRepository.serializeScenes(scenes),
            activeAspectRatio = AspectRatioFormat.LANDSCAPE_16_9.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return ImportResult.Success(bundle, project)
    }

    private fun parsePackageJsonManifest(root: JSONObject): ImportResult {
        val name = root.optString("name", "Imported Project")
        val description = root.optString("description", "Open source software project")
        val repository = when {
            root.optJSONObject("repository") != null -> root.getJSONObject("repository").optString("url", "")
            root.optString("repository").isNotEmpty() -> root.optString("repository")
            else -> "https://github.com/project/$name"
        }

        val deps = mutableListOf<String>()
        root.optJSONObject("dependencies")?.let { dObj ->
            for (key in dObj.keys()) {
                deps.add(key)
            }
        }

        val brandProfile = BrandProfile(
            name = formatPackageName(name),
            tagline = description,
            description = description,
            primaryColorHex = "#6366F1",
            secondaryColorHex = "#8B5CF6",
            accentColorHex = "#06B6D4",
            targetAudience = "Software Engineers & Developers",
            repoPathOrUrl = repository
        )

        val scenes = generateFallbackScenes(brandProfile.name, brandProfile)
        val project = PromoProject(
            id = System.currentTimeMillis(),
            title = brandProfile.name,
            tagline = description,
            repoUrl = repository,
            brandName = brandProfile.name,
            primaryColorHex = brandProfile.primaryColorHex,
            secondaryColorHex = brandProfile.secondaryColorHex,
            accentColorHex = brandProfile.accentColorHex,
            techStackJson = JSONArray(deps.take(8)).toString(),
            featuresJson = JSONArray(listOf("High-performance core", "Zero dependencies", "Modular architecture")).toString(),
            narrativeTemplate = NarrativeTemplate.DEMO_FIRST.name,
            targetDurationSeconds = 30,
            voiceActor = VoiceActor.ADAM.name,
            musicTrack = MusicTrackOption.INSPIRED_AMBIENT.name,
            transitionStyle = TransitionStyle.METALLIC_SWOOSH.name,
            scenesJson = PromoRepository.serializeScenes(scenes),
            activeAspectRatio = AspectRatioFormat.LANDSCAPE_16_9.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val bundle = ProjectBundle(
            projectId = project.id,
            projectTitle = project.title,
            brandProfile = brandProfile,
            scanManifest = DeterministicScanManifest(
                appName = brandProfile.name,
                rawRepoUrl = repository,
                normalizedRepoName = name,
                technologies = deps.take(8),
                keyFeatures = listOf("Automated pipeline", "Developer-first CLI"),
                userPersonas = listOf("Developers"),
                notableFiles = listOf("package.json", "README.md"),
                claimsWithEvidence = emptyList(),
                excludedFiles = emptyList()
            ),
            creativeBrief = CreativeBrief(
                title = project.title,
                hook = "Introducing ${project.title}: $description",
                coreBenefit = description,
                targetAudience = brandProfile.targetAudience,
                keyClaims = emptyList(),
                brandColors = listOf("#6366F1", "#8B5CF6", "#06B6D4"),
                callToAction = "Install with npm install $name"
            ),
            scenes = scenes,
            claims = emptyList(),
            licensingManifest = emptyList(),
            voiceConfig = "ElevenLabs HD Neural"
        )

        return ImportResult.Success(bundle, project)
    }

    private fun parseGenericProjectJson(root: JSONObject): ImportResult {
        val title = root.optString("title", root.optString("name", "Imported Project"))
        val brandProfile = BrandProfile(
            name = title,
            tagline = root.optString("tagline", root.optString("description", "Code to video")),
            description = root.optString("description", "Automated code-to-video showcase"),
            primaryColorHex = root.optString("primaryColor", "#6366F1"),
            secondaryColorHex = root.optString("secondaryColor", "#8B5CF6"),
            accentColorHex = root.optString("accentColor", "#06B6D4"),
            targetAudience = "Developers",
            repoPathOrUrl = root.optString("url", root.optString("repoUrl", ""))
        )

        val scenes = generateFallbackScenes(title, brandProfile)
        val project = PromoProject(
            id = System.currentTimeMillis(),
            title = title,
            tagline = brandProfile.tagline,
            repoUrl = brandProfile.repoPathOrUrl,
            brandName = brandProfile.name,
            primaryColorHex = brandProfile.primaryColorHex,
            secondaryColorHex = brandProfile.secondaryColorHex,
            accentColorHex = brandProfile.accentColorHex,
            techStackJson = "[\"TypeScript\", \"React\", \"Remotion\"]",
            featuresJson = "[\"Instant Launch Video\", \"Automated Voiceover\"]",
            narrativeTemplate = NarrativeTemplate.PROBLEM_STACK.name,
            targetDurationSeconds = 30,
            voiceActor = VoiceActor.RACHEL.name,
            musicTrack = MusicTrackOption.INSPIRED_AMBIENT.name,
            transitionStyle = TransitionStyle.METALLIC_SWOOSH.name,
            scenesJson = PromoRepository.serializeScenes(scenes),
            activeAspectRatio = AspectRatioFormat.LANDSCAPE_16_9.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val bundle = ProjectBundle(
            projectId = project.id,
            projectTitle = title,
            brandProfile = brandProfile,
            scanManifest = DeterministicScanManifest(
                appName = title,
                rawRepoUrl = brandProfile.repoPathOrUrl,
                normalizedRepoName = title.lowercase(),
                technologies = listOf("Kotlin", "Compose"),
                keyFeatures = listOf("Automated scenes"),
                userPersonas = listOf("Developers"),
                notableFiles = emptyList(),
                claimsWithEvidence = emptyList(),
                excludedFiles = emptyList()
            ),
            creativeBrief = CreativeBrief(
                title = title,
                hook = "Stop wasting time on video editing",
                coreBenefit = brandProfile.tagline,
                targetAudience = "Tech Enthusiasts",
                keyClaims = emptyList(),
                brandColors = listOf(brandProfile.primaryColorHex, brandProfile.secondaryColorHex),
                callToAction = "Check out the repo"
            ),
            scenes = scenes,
            claims = emptyList(),
            licensingManifest = emptyList(),
            voiceConfig = "ElevenLabs HD Neural"
        )

        return ImportResult.Success(bundle, project)
    }

    private fun generateFallbackScenes(title: String, brand: BrandProfile): List<PromoScene> {
        return listOf(
            PromoScene(
                orderIndex = 0,
                title = "The Frustration",
                subtitle = "Manual Video Production",
                voiceover = "Why spend hours editing videos when your codebase can film its own launch promo?",
                emotionalPreset = EmotionalPreset.RAGE,
                durationSeconds = 3.5f,
                visualType = SceneVisualType.HOOK_FRUSTRATION,
                accentTag = "THE PROBLEM",
                badgeTags = listOf("Manual Editing", "Slow Turnaround", "Outdated Demos")
            ),
            PromoScene(
                orderIndex = 1,
                title = title,
                subtitle = brand.tagline,
                voiceover = "Meet $title. The automated developer video director for modern engineering teams.",
                emotionalPreset = EmotionalPreset.CONFIDENT,
                durationSeconds = 4.0f,
                visualType = SceneVisualType.CODE_TERMINAL,
                accentTag = "THE REVEAL",
                codeSnippet = "npx devdirector generate \\\n  --repo ${brand.repoPathOrUrl.ifEmpty { "github.com/org/repo" }} \\\n  --theme cinematic",
                badgeTags = listOf("Automated", "TypeScript", "Remotion")
            ),
            PromoScene(
                orderIndex = 2,
                title = "Engineered for Velocity",
                subtitle = "High-Converting Promo Formats",
                voiceover = "Export dual 16:9 and 9:16 aspect ratios with synced neural voiceover in seconds.",
                emotionalPreset = EmotionalPreset.WARM,
                durationSeconds = 4.0f,
                visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                accentTag = "DUAL FORMAT",
                badgeTags = listOf("16:9 Landscape", "9:16 Portrait", "4K Ready")
            ),
            PromoScene(
                orderIndex = 3,
                title = "Get Started Today",
                subtitle = "Free & Open Source",
                voiceover = "Ship your launch promo with one command. Check out the repository now.",
                emotionalPreset = EmotionalPreset.CONFIDENT,
                durationSeconds = 3.5f,
                visualType = SceneVisualType.LOGO_REVEAL_CTA,
                accentTag = "LAUNCH",
                ctaButtonText = "Star on GitHub",
                ctaUrl = brand.repoPathOrUrl.ifEmpty { "https://github.com" },
                badgeTags = listOf("MIT License", "Community Driven")
            )
        )
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val str = arr.optString(i)
            if (str.isNotBlank()) list.add(str)
        }
        return list
    }

    private fun formatPackageName(raw: String): String {
        return raw.removePrefix("@")
            .split("/", "-", "_")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
