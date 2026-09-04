package com.example.data.local

import com.example.data.model.AspectRatioFormat
import com.example.data.model.EmotionalPreset
import com.example.data.model.MusicTrackOption
import com.example.data.model.NarrativeTemplate
import com.example.data.model.PromoProject
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.data.model.TransitionStyle
import com.example.data.model.VoiceActor
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class PromoRepository(private val promoDao: PromoDao) {

    val allProjects: Flow<List<PromoProject>> = promoDao.getAllProjects()

    suspend fun getProjectById(id: Long): PromoProject? {
        return promoDao.getProjectById(id)
    }

    suspend fun saveProject(project: PromoProject): Long {
        return promoDao.insertProject(project)
    }

    suspend fun updateProject(project: PromoProject) {
        promoDao.updateProject(project)
    }

    suspend fun deleteProject(project: PromoProject) {
        promoDao.deleteProject(project)
    }

    suspend fun deleteProjectById(id: Long) {
        promoDao.deleteProjectById(id)
    }

    suspend fun updateProjectSyncState(
        projectId: Long,
        syncState: com.example.data.model.SyncState,
        conflictOutcome: String? = null
    ) {
        val project = promoDao.getProjectById(projectId) ?: return
        val updated = project.copy(
            syncState = syncState.name,
            lastSyncAttemptAt = System.currentTimeMillis(),
            syncConflictOutcome = conflictOutcome,
            syncRetryCount = if (syncState == com.example.data.model.SyncState.FAILED) project.syncRetryCount + 1 else project.syncRetryCount,
            updatedAt = System.currentTimeMillis()
        )
        promoDao.updateProject(updated)
    }

    suspend fun updateProjectManifestAndBrief(
        projectId: Long,
        manifestJson: String,
        briefJson: String,
        claimsJson: String
    ) {
        val project = promoDao.getProjectById(projectId) ?: return
        val updated = project.copy(
            scanManifestJson = manifestJson,
            creativeBriefJson = briefJson,
            claimEvidencesJson = claimsJson,
            updatedAt = System.currentTimeMillis()
        )
        promoDao.updateProject(updated)
    }

    suspend fun markRawSourceDeleted(projectId: Long) {
        val project = promoDao.getProjectById(projectId) ?: return
        val updated = project.copy(
            rawSourceDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
        promoDao.updateProject(updated)
    }

    suspend fun seedDefaultsIfEmpty() {
        if (promoDao.getProjectCount() == 0) {
            val sampleProjects = getSampleProjects()
            for (p in sampleProjects) {
                promoDao.insertProject(p)
            }
        }
    }

    companion object {
        fun serializeScenes(scenes: List<PromoScene>): String {
            val jsonArray = JSONArray()
            for (scene in scenes) {
                val obj = JSONObject().apply {
                    put("id", scene.id)
                    put("orderIndex", scene.orderIndex)
                    put("title", scene.title)
                    put("subtitle", scene.subtitle)
                    put("voiceover", scene.voiceover)
                    put("emotionalPreset", scene.emotionalPreset.name)
                    put("durationSeconds", scene.durationSeconds.toDouble())
                    put("visualType", scene.visualType.name)
                    put("accentTag", scene.accentTag)
                    put("codeSnippet", scene.codeSnippet ?: "")
                    put("metricNumber", scene.metricNumber ?: "")
                    put("metricLabel", scene.metricLabel ?: "")
                    put("ctaButtonText", scene.ctaButtonText ?: "")
                    put("ctaUrl", scene.ctaUrl ?: "")
                    put("imagePrompt", scene.imagePrompt ?: "")
                    put("generatedImageUrl", scene.generatedImageUrl ?: "")
                    put("generatedVideoUrl", scene.generatedVideoUrl ?: "")

                    val badges = JSONArray()
                    scene.badgeTags.forEach { badges.put(it) }
                    put("badgeTags", badges)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun deserializeScenes(jsonStr: String): List<PromoScene> {
            val list = mutableListOf<PromoScene>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val badges = mutableListOf<String>()
                    val badgesArray = obj.optJSONArray("badgeTags")
                    if (badgesArray != null) {
                        for (j in 0 until badgesArray.length()) {
                            badges.add(badgesArray.getString(j))
                        }
                    }

                    list.add(
                        PromoScene(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            orderIndex = obj.optInt("orderIndex", i),
                            title = obj.optString("title", "Scene ${i + 1}"),
                            subtitle = obj.optString("subtitle", ""),
                            voiceover = obj.optString("voiceover", ""),
                            emotionalPreset = try {
                                EmotionalPreset.valueOf(obj.optString("emotionalPreset", EmotionalPreset.CONFIDENT.name))
                            } catch (_: Exception) { EmotionalPreset.CONFIDENT },
                            durationSeconds = obj.optDouble("durationSeconds", 3.5).toFloat(),
                            visualType = try {
                                SceneVisualType.valueOf(obj.optString("visualType", SceneVisualType.BROWSER_MOCKUP_3D.name))
                            } catch (_: Exception) { SceneVisualType.BROWSER_MOCKUP_3D },
                            accentTag = obj.optString("accentTag", ""),
                            codeSnippet = obj.optString("codeSnippet", "").ifEmpty { null },
                            metricNumber = obj.optString("metricNumber", "").ifEmpty { null },
                            metricLabel = obj.optString("metricLabel", "").ifEmpty { null },
                            badgeTags = badges,
                            ctaButtonText = obj.optString("ctaButtonText", "").ifEmpty { null },
                            ctaUrl = obj.optString("ctaUrl", "").ifEmpty { null },
                            imagePrompt = obj.optString("imagePrompt", "").ifEmpty { null },
                            generatedImageUrl = obj.optString("generatedImageUrl", "").ifEmpty { null },
                            generatedVideoUrl = obj.optString("generatedVideoUrl", "").ifEmpty { null }
                        )
                    )
                }
            } catch (_: Exception) {
                // Return empty if parsing fails
            }
            return list
        }

        fun getSampleProjects(): List<PromoProject> {
            // Project 1: PromoVideo Skill (The featured project in the prompt)
            val promoVideoScenes = listOf(
                PromoScene(
                    orderIndex = 0,
                    title = "Turn Code into Video",
                    subtitle = "The Frustration Hook",
                    voiceover = "Spending forty hours editing product demo videos instead of shipping code?! Are you serious right now?",
                    emotionalPreset = EmotionalPreset.RAGE,
                    durationSeconds = 3.2f,
                    visualType = SceneVisualType.HOOK_FRUSTRATION,
                    accentTag = "THE PAIN",
                    badgeTags = listOf("40hrs Editing", "Expensive Video Studios", "Outdated Demos")
                ),
                PromoScene(
                    orderIndex = 1,
                    title = "One Command Automation",
                    subtitle = "Automated Codebase Scanner",
                    voiceover = "What if your codebase could automatically film its own launch video in sixty seconds?",
                    emotionalPreset = EmotionalPreset.WHISPER,
                    durationSeconds = 3.5f,
                    visualType = SceneVisualType.CODE_TERMINAL,
                    accentTag = "THE REVEAL",
                    codeSnippet = "npx skills add remotion-dev/skills\nnpx skills add AKCodez/promo-video-skill\n\n# Run promo video generator\nclaude \"Create 60s dark mode promo video\"",
                    badgeTags = listOf("Claude Code", "Remotion", "ElevenLabs")
                ),
                PromoScene(
                    orderIndex = 2,
                    title = "3D Browser & Spring Physics",
                    subtitle = "Dual Format Renders",
                    voiceover = "Smart brand discovery scans your logo, colors, and dependencies — generating fluid 3D mockups in both landscape and portrait.",
                    emotionalPreset = EmotionalPreset.CONFIDENT,
                    durationSeconds = 4.0f,
                    visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                    accentTag = "DUAL FORMAT",
                    badgeTags = listOf("1920 × 1080 (16:9)", "1080 × 1920 (9:16)", "Metallic Swoosh")
                ),
                PromoScene(
                    orderIndex = 3,
                    title = "100% Production Ready",
                    subtitle = "Instant Multiplatform Reach",
                    voiceover = "Join thousands of developers turning git repositories into high-converting launch videos.",
                    emotionalPreset = EmotionalPreset.WARM,
                    durationSeconds = 3.2f,
                    visualType = SceneVisualType.METRIC_COUNTER,
                    accentTag = "PROOF",
                    metricNumber = "50,000+",
                    metricLabel = "Videos Rendered on Remotion",
                    badgeTags = listOf("4K Output", "Whisper Timing", "Zero Overlap")
                ),
                PromoScene(
                    orderIndex = 4,
                    title = "Get Started Now",
                    subtitle = "Open Source & Free",
                    voiceover = "Try promo-video-skill today. One prompt. Rendered and ready to post.",
                    emotionalPreset = EmotionalPreset.DRAMATIC,
                    durationSeconds = 3.5f,
                    visualType = SceneVisualType.LOGO_REVEAL_CTA,
                    accentTag = "CALL TO ACTION",
                    ctaButtonText = "Install Promo Skill",
                    ctaUrl = "github.com/AKCodez/promo-video-skill",
                    badgeTags = listOf("MIT License", "Open Source")
                )
            )

            // Project 2: FastSolve Chrome Extension (from prompt example: Rage Hook narrative, Adam voice, Dark theme)
            val fastSolveScenes = listOf(
                PromoScene(
                    orderIndex = 0,
                    title = "Stuck on Complex Problems?",
                    subtitle = "The Frustration Hook",
                    voiceover = "Three hours searching StackOverflow for a bug that should have taken three seconds?!",
                    emotionalPreset = EmotionalPreset.RAGE,
                    durationSeconds = 3.0f,
                    visualType = SceneVisualType.HOOK_FRUSTRATION,
                    accentTag = "DEVELOPER RAGE",
                    badgeTags = listOf("Infinite Tab Sprawl", "Broken Docs", "Lost Hours")
                ),
                PromoScene(
                    orderIndex = 1,
                    title = "Instant Code Intelligence",
                    subtitle = "Zero-Friction Discovery",
                    voiceover = "What if you had an AI debugging genius sitting directly inside your Chrome browser?",
                    emotionalPreset = EmotionalPreset.WHISPER,
                    durationSeconds = 3.4f,
                    visualType = SceneVisualType.CODE_TERMINAL,
                    accentTag = "THE SECRET",
                    codeSnippet = "// FastSolve Live Diagnostic\nconst fix = await fastsolve.inspect(error);\nconsole.log(fix.solution); // Resolved in 12ms",
                    badgeTags = listOf("Chrome Extension", "Manifest V3", "Instant Fix")
                ),
                PromoScene(
                    orderIndex = 2,
                    title = "Live Browser Automation",
                    subtitle = "Seamless Overlay UX",
                    voiceover = "FastSolve scans your console errors and highlights the exact fix with zero context switching.",
                    emotionalPreset = EmotionalPreset.CONFIDENT,
                    durationSeconds = 3.8f,
                    visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                    accentTag = "FEATURE DEMO",
                    badgeTags = listOf("1-Click Fix", "AI Grounding", "Zero Setup")
                ),
                PromoScene(
                    orderIndex = 3,
                    title = "Visit FastSolve.app",
                    subtitle = "Free for Developers",
                    voiceover = "Visit fastsolve.app today. Fix bugs at the speed of thought.",
                    emotionalPreset = EmotionalPreset.DRAMATIC,
                    durationSeconds = 3.5f,
                    visualType = SceneVisualType.LOGO_REVEAL_CTA,
                    accentTag = "CALL TO ACTION",
                    ctaButtonText = "Visit fastsolve.app",
                    ctaUrl = "https://fastsolve.app",
                    badgeTags = listOf("Install Free", "5-Star Rating")
                )
            )

            // Project 3: Remotion Video Framework (Demo First narrative)
            val remotionScenes = listOf(
                PromoScene(
                    orderIndex = 0,
                    title = "Code Your Videos with React",
                    subtitle = "The Magic Moment",
                    voiceover = "Programmatic video creation in pure React. Every frame is a component.",
                    emotionalPreset = EmotionalPreset.CONFIDENT,
                    durationSeconds = 3.6f,
                    visualType = SceneVisualType.CODE_TERMINAL,
                    accentTag = "DEMO FIRST",
                    codeSnippet = "export const MyVideo = () => {\n  const frame = useCurrentFrame();\n  const opacity = interpolate(frame, [0, 30], [0, 1]);\n  return <Title style={{ opacity }} />;\n};",
                    badgeTags = listOf("React 19", "Three.js 3D", "WebCodecs")
                ),
                PromoScene(
                    orderIndex = 1,
                    title = "Interactive Canvas Studio",
                    subtitle = "Zero Video Editing Timeline",
                    voiceover = "No clunky timeline editors. Just version control, CI/CD pipelines, and cloud scale rendering.",
                    emotionalPreset = EmotionalPreset.CONFIDENT,
                    durationSeconds = 3.8f,
                    visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                    accentTag = "CORE POWERS",
                    badgeTags = listOf("Remotion Studio", "Cloud Render", "SSR Ready")
                ),
                PromoScene(
                    orderIndex = 2,
                    title = "Unmatched Rendering Speed",
                    subtitle = "Million Frame Scale",
                    voiceover = "Used by over one hundred thousand developers worldwide to render millions of videos every day.",
                    emotionalPreset = EmotionalPreset.WARM,
                    durationSeconds = 3.4f,
                    visualType = SceneVisualType.METRIC_COUNTER,
                    accentTag = "PROVEN AT SCALE",
                    metricNumber = "100,000+",
                    metricLabel = "Active Builders Worldwide",
                    badgeTags = listOf("Distributed Render", "Fast Lambda", "Low Latency")
                ),
                PromoScene(
                    orderIndex = 3,
                    title = "Start Building with Remotion",
                    subtitle = "npm create video",
                    voiceover = "Start coding your videos today. Run npm create video to launch.",
                    emotionalPreset = EmotionalPreset.DRAMATIC,
                    durationSeconds = 3.5f,
                    visualType = SceneVisualType.LOGO_REVEAL_CTA,
                    accentTag = "LAUNCH",
                    ctaButtonText = "npm create video",
                    ctaUrl = "https://remotion.dev",
                    badgeTags = listOf("Free & Open Source")
                )
            )

            return listOf(
                PromoProject(
                    id = 1,
                    title = "PromoVideo AI Skill",
                    tagline = "Turn any codebase into a professional promo video in one command",
                    repoUrl = "AKCodez/promo-video-skill",
                    brandName = "PromoVideo",
                    primaryColorHex = "#6366F1",
                    secondaryColorHex = "#8B5CF6",
                    accentColorHex = "#F59E0B",
                    techStackJson = "[\"Claude Code\",\"Remotion\",\"ElevenLabs\",\"TypeScript\",\"Tailwind\"]",
                    featuresJson = "[\"One command generation\",\"Landscape 16:9 + Portrait 9:16\",\"Emotional voiceover presets\",\"Codebase auto-discovery\"]",
                    narrativeTemplate = NarrativeTemplate.RAGE_HOOK.name,
                    targetDurationSeconds = 60,
                    voiceActor = VoiceActor.MATILDA.name,
                    musicTrack = MusicTrackOption.INSPIRED_AMBIENT.name,
                    transitionStyle = TransitionStyle.METALLIC_SWOOSH.name,
                    scenesJson = serializeScenes(promoVideoScenes),
                    activeAspectRatio = AspectRatioFormat.LANDSCAPE_16_9.name,
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis() - 3600000
                ),
                PromoProject(
                    id = 2,
                    title = "FastSolve Chrome Extension",
                    tagline = "Instant AI error diagnostics directly inside developer console",
                    repoUrl = "fastsolve/extension-core",
                    brandName = "FastSolve",
                    primaryColorHex = "#06B6D4",
                    secondaryColorHex = "#3B82F6",
                    accentColorHex = "#10B981",
                    techStackJson = "[\"Chrome Extension\",\"TypeScript\",\"Gemini 3\",\"Tailwind\"]",
                    featuresJson = "[\"Inline console debugging\",\"Zero context switching\",\"1-click fix generation\",\"Private local mode\"]",
                    narrativeTemplate = NarrativeTemplate.RAGE_HOOK.name,
                    targetDurationSeconds = 30,
                    voiceActor = VoiceActor.ADAM.name,
                    musicTrack = MusicTrackOption.MOTIVATIONAL_DAY.name,
                    transitionStyle = TransitionStyle.METALLIC_SWOOSH.name,
                    scenesJson = serializeScenes(fastSolveScenes),
                    activeAspectRatio = AspectRatioFormat.LANDSCAPE_16_9.name,
                    createdAt = System.currentTimeMillis() - 43200000,
                    updatedAt = System.currentTimeMillis() - 1800000
                ),
                PromoProject(
                    id = 3,
                    title = "Remotion Framework",
                    tagline = "Make videos programmatically with React components",
                    repoUrl = "remotion-dev/remotion",
                    brandName = "Remotion",
                    primaryColorHex = "#8B5CF6",
                    secondaryColorHex = "#EC4899",
                    accentColorHex = "#3B82F6",
                    techStackJson = "[\"React\",\"TypeScript\",\"WebCodecs\",\"FFmpeg\"]",
                    featuresJson = "[\"React component videos\",\"Cloud rendering at scale\",\"Audio & waveform sync\",\"3D Three.js canvas\"]",
                    narrativeTemplate = NarrativeTemplate.DEMO_FIRST.name,
                    targetDurationSeconds = 60,
                    voiceActor = VoiceActor.DANIEL.name,
                    musicTrack = MusicTrackOption.UPBEAT_CORPORATE.name,
                    transitionStyle = TransitionStyle.SPRING_WIPE.name,
                    scenesJson = serializeScenes(remotionScenes),
                    activeAspectRatio = AspectRatioFormat.PORTRAIT_9_16.name,
                    createdAt = System.currentTimeMillis() - 120000000,
                    updatedAt = System.currentTimeMillis() - 600000
                )
            )
        }
    }
}
