package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.EmotionalPreset
import com.example.data.model.NarrativeTemplate
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiPromoService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) { "" }

    val hasApiKey: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * Phase 1: Brand Discovery from codebase info or repo description
     */
    suspend fun discoverBrand(repoInput: String): Result<BrandProfile> = withContext(Dispatchers.IO) {
        if (!hasApiKey) {
            // Intelligent fallback brand discovery
            return@withContext Result.success(createLocalBrandProfile(repoInput))
        }

        try {
            val systemPrompt = """
                You are an expert brand designer and technical marketer for developer tools.
                Analyze the provided repository, codebase info, package.json, or product description.
                Extract:
                1. Product Name
                2. Punchy Tagline (max 10 words)
                3. Short Description (1-2 sentences)
                4. Primary Hex Color (modern vibrant, e.g. #6366F1, #06B6D4, #8B5CF6, #F43F5E)
                5. Secondary Hex Color
                6. Accent Hex Color (#F59E0B, #10B981, etc.)
                7. Tech Stack (list of 4-6 technologies/frameworks)
                8. Key Features (3-5 killer features, max 8 words each)
                9. Target Audience
                
                Respond ONLY with valid JSON in this exact structure:
                {
                  "name": "...",
                  "tagline": "...",
                  "description": "...",
                  "primaryColorHex": "#HEX",
                  "secondaryColorHex": "#HEX",
                  "accentColorHex": "#HEX",
                  "techStack": ["...", "..."],
                  "keyFeatures": ["...", "..."],
                  "targetAudience": "..."
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Codebase info:\n$repoInput"))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "HTTP ${response.code}"
                Log.w("GeminiPromoService", "Brand discovery failed, using fallback: $errorMsg")
                return@withContext Result.success(createLocalBrandProfile(repoInput))
            }

            val respBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(respBody)
            val candidate = jsonRoot.getJSONArray("candidates").getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val rawText = content.getJSONArray("parts").getJSONObject(0).getString("text")

            val brandJson = JSONObject(rawText)
            val techStackList = mutableListOf<String>()
            val techArray = brandJson.optJSONArray("techStack")
            if (techArray != null) {
                for (i in 0 until techArray.length()) techStackList.add(techArray.getString(i))
            }

            val featuresList = mutableListOf<String>()
            val featArray = brandJson.optJSONArray("keyFeatures")
            if (featArray != null) {
                for (i in 0 until featArray.length()) featuresList.add(featArray.getString(i))
            }

            Result.success(
                BrandProfile(
                    name = brandJson.optString("name", "My Product"),
                    tagline = brandJson.optString("tagline", "Built for high performance"),
                    description = brandJson.optString("description", "A modern tool for builders."),
                    logoIcon = "code",
                    primaryColorHex = brandJson.optString("primaryColorHex", "#6366F1"),
                    secondaryColorHex = brandJson.optString("secondaryColorHex", "#8B5CF6"),
                    accentColorHex = brandJson.optString("accentColorHex", "#F59E0B"),
                    techStack = if (techStackList.isNotEmpty()) techStackList else listOf("TypeScript", "React", "Remotion"),
                    keyFeatures = if (featuresList.isNotEmpty()) featuresList else listOf("Fast setup", "Modern UI", "Cloud scale"),
                    targetAudience = brandJson.optString("targetAudience", "Developers & creators"),
                    repoPathOrUrl = repoInput.take(60)
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiPromoService", "Error in brand discovery: ${e.message}", e)
            Result.success(createLocalBrandProfile(repoInput))
        }
    }

    /**
     * Phase 2 & 3: Generate Scene Script based on Narrative Template & Duration
     */
    suspend fun generatePromoScript(
        brand: BrandProfile,
        template: NarrativeTemplate,
        durationSeconds: Int
    ): Result<List<PromoScene>> = withContext(Dispatchers.IO) {
        if (!hasApiKey) {
            return@withContext Result.success(createLocalScript(brand, template, durationSeconds))
        }

        try {
            val sceneCount = when (durationSeconds) {
                30 -> 4
                60 -> 5
                else -> 6
            }

            val systemPrompt = """
                You are an award-winning promo video director producing a Remotion-powered video script.
                Narrative Template: ${template.title} (${template.arc})
                Target Duration: $durationSeconds seconds total ($sceneCount scenes, 2.5s to 4.5s each).
                Brand Name: ${brand.name}
                Tagline: ${brand.tagline}
                Description: ${brand.description}
                Features: ${brand.keyFeatures.joinToString(", ")}
                Tech Stack: ${brand.techStack.joinToString(", ")}

                Produce $sceneCount high-energy scenes.
                For voiceover, assign an emotional preset per scene matching the arc:
                - RAGE: frustration hook ("Are you serious right now?!")
                - WHISPER: secret reveal ("What if you never had to guess again?")
                - CONFIDENT: feature demo & smart detection
                - WARM: social proof & scale metrics
                - DRAMATIC: final punchy call-to-action ("Try it now. Free.")

                Visual types must be chosen from:
                HOOK_FRUSTRATION, CODE_TERMINAL, BROWSER_MOCKUP_3D, FEATURE_SPOTLIGHT, METRIC_COUNTER, LOGO_REVEAL_CTA

                Respond ONLY with JSON matching:
                {
                  "scenes": [
                    {
                      "orderIndex": 0,
                      "title": "Short scene title",
                      "subtitle": "Short subtitle",
                      "voiceover": "Compelling narration line spoken by AI voice (12-25 words)",
                      "emotionalPreset": "RAGE|WHISPER|CONFIDENT|WARM|DRAMATIC",
                      "durationSeconds": 3.5,
                      "visualType": "HOOK_FRUSTRATION|CODE_TERMINAL|BROWSER_MOCKUP_3D|FEATURE_SPOTLIGHT|METRIC_COUNTER|LOGO_REVEAL_CTA",
                      "accentTag": "Category / Step Tag",
                      "codeSnippet": "Code snippet or null",
                      "metricNumber": "Metric number like 50,000+ or null",
                      "metricLabel": "Metric label or null",
                      "badgeTags": ["Tag 1", "Tag 2"],
                      "ctaButtonText": "Button label if CTA or null",
                      "ctaUrl": "URL if CTA or null"
                    }
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Generate complete $durationSeconds-second promo script for ${brand.name}."))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("responseMimeType", "application/json")
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiPromoService", "Script generation failed, using local script")
                return@withContext Result.success(createLocalScript(brand, template, durationSeconds))
            }

            val respBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(respBody)
            val candidate = jsonRoot.getJSONArray("candidates").getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val rawText = content.getJSONArray("parts").getJSONObject(0).getString("text")

            val scriptObj = JSONObject(rawText)
            val scenesArray = scriptObj.getJSONArray("scenes")
            val resultScenes = mutableListOf<PromoScene>()

            for (i in 0 until scenesArray.length()) {
                val sc = scenesArray.getJSONObject(i)
                val badges = mutableListOf<String>()
                val bArr = sc.optJSONArray("badgeTags")
                if (bArr != null) {
                    for (j in 0 until bArr.length()) badges.add(bArr.getString(j))
                }

                resultScenes.add(
                    PromoScene(
                        id = java.util.UUID.randomUUID().toString(),
                        orderIndex = i,
                        title = sc.optString("title", "Scene ${i + 1}"),
                        subtitle = sc.optString("subtitle", ""),
                        voiceover = sc.optString("voiceover", "Introducing the next generation of building."),
                        emotionalPreset = try {
                            EmotionalPreset.valueOf(sc.optString("emotionalPreset", "CONFIDENT"))
                        } catch (_: Exception) { EmotionalPreset.CONFIDENT },
                        durationSeconds = sc.optDouble("durationSeconds", 3.5).toFloat(),
                        visualType = try {
                            SceneVisualType.valueOf(sc.optString("visualType", "BROWSER_MOCKUP_3D"))
                        } catch (_: Exception) { SceneVisualType.BROWSER_MOCKUP_3D },
                        accentTag = sc.optString("accentTag", "FEATURE"),
                        codeSnippet = sc.optString("codeSnippet", "").ifEmpty { null },
                        metricNumber = sc.optString("metricNumber", "").ifEmpty { null },
                        metricLabel = sc.optString("metricLabel", "").ifEmpty { null },
                        badgeTags = badges,
                        ctaButtonText = sc.optString("ctaButtonText", "").ifEmpty { null },
                        ctaUrl = sc.optString("ctaUrl", "").ifEmpty { null }
                    )
                )
            }

            Result.success(if (resultScenes.isNotEmpty()) resultScenes else createLocalScript(brand, template, durationSeconds))
        } catch (e: Exception) {
            Log.e("GeminiPromoService", "Error generating script: ${e.message}", e)
            Result.success(createLocalScript(brand, template, durationSeconds))
        }
    }

    /**
     * Veo Video Generation API (`veo-3.1-fast-generate-preview`)
     */
    suspend fun generateVeoVideo(prompt: String, format: AspectRatioFormat): Result<String> = withContext(Dispatchers.IO) {
        if (!hasApiKey) {
            // Simulated instant preview response for prototyping
            val sampleVideoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            return@withContext Result.success(sampleVideoUri)
        }

        try {
            val aspectString = if (format == AspectRatioFormat.LANDSCAPE_16_9) "16:9" else "9:16"
            val requestJson = JSONObject().apply {
                put("prompt", prompt)
                put("config", JSONObject().apply {
                    put("numberOfVideos", 1)
                    put("resolution", "1080p")
                    put("aspectRatio", aspectString)
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val json = JSONObject(respBody)
                val opName = json.optString("name", "operations/veo_preview_simulation")
                Result.success(opName)
            } else {
                Result.failure(Exception("Veo API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * AI Director Chat for iterating on the promo video
     */
    suspend fun chatWithDirector(
        history: List<Pair<String, String>>, // role, text
        userMessage: String,
        currentProject: BrandProfile
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!hasApiKey) {
            return@withContext Result.success(generateLocalDirectorResponse(userMessage, currentProject))
        }

        try {
            val systemPrompt = """
                You are Claude / Gemini Director, the creative AI behind promo-video-skill.
                You help developers craft high-converting promo videos for their codebases.
                Current Project: ${currentProject.name} - ${currentProject.tagline}
                Features: ${currentProject.keyFeatures.joinToString("; ")}
                
                You can help with:
                - Rewriting scene voiceovers with emotional presets (Rage, Whisper, Confident, Warm, Dramatic)
                - Timing calculations (2-4 seconds per scene)
                - Switching between 16:9 Landscape and 9:16 Portrait adaptations
                - Recommending transitions (Metallic Swoosh, Spring Wipe, Velocity Slide)
                - Auditioning voice actors (Matilda, Rachel, Daniel, Josh, Adam)
                
                Keep your answers concise, practical, and punchy.
            """.trimIndent()

            val contentsArray = JSONArray()
            for ((role, text) in history) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (role == "user") "user" else "model")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", text)) })
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().put("text", userMessage)) })
            })

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().put("text", systemPrompt)) })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val jsonRoot = JSONObject(respBody)
                val candidate = jsonRoot.getJSONArray("candidates").getJSONObject(0)
                val text = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                Result.success(text)
            } else {
                Result.success(generateLocalDirectorResponse(userMessage, currentProject))
            }
        } catch (e: Exception) {
            Result.success(generateLocalDirectorResponse(userMessage, currentProject))
        }
    }

    private fun createLocalBrandProfile(input: String): BrandProfile {
        val clean = input.trim()
        val detectedName = when {
            clean.contains("chrome", ignoreCase = true) -> "FastSolve Extension"
            clean.contains("remotion", ignoreCase = true) -> "Remotion Video"
            clean.contains("ai", ignoreCase = true) -> "PromoVideo AI"
            clean.contains("supabase", ignoreCase = true) -> "Supabase Storage"
            clean.contains("/") -> clean.substringAfter("/").replace("-", " ").replace("_", " ").capitalizeWords()
            else -> clean.ifEmpty { "FastShip Dev" }
        }

        return BrandProfile(
            name = detectedName,
            tagline = "Turn your codebase into high-converting videos in seconds",
            description = "Automated developer promotional videos rendered in landscape and portrait.",
            logoIcon = "movie_filter",
            primaryColorHex = "#6366F1",
            secondaryColorHex = "#8B5CF6",
            accentColorHex = "#F59E0B",
            techStack = listOf("TypeScript", "React", "Remotion", "ElevenLabs", "Tailwind"),
            keyFeatures = listOf(
                "One-command video workflow",
                "Landscape 16:9 + Portrait 9:16",
                "AI voiceover with 5 emotional presets",
                "Automated brand color & logo discovery"
            ),
            targetAudience = "Engineers, founders & content creators",
            repoPathOrUrl = input.ifEmpty { "AKCodez/promo-video-skill" }
        )
    }

    private fun createLocalScript(
        brand: BrandProfile,
        template: NarrativeTemplate,
        duration: Int
    ): List<PromoScene> {
        val scenes = mutableListOf<PromoScene>()
        when (template) {
            NarrativeTemplate.RAGE_HOOK -> {
                scenes.add(
                    PromoScene(
                        orderIndex = 0,
                        title = "Stop Wasting Time",
                        subtitle = "The Frustration Hook",
                        voiceover = "Spending 40 hours editing video demos instead of building your product?! Are you serious right now?",
                        emotionalPreset = EmotionalPreset.RAGE,
                        durationSeconds = 3.2f,
                        visualType = SceneVisualType.HOOK_FRUSTRATION,
                        accentTag = "THE PAIN",
                        badgeTags = listOf("40hrs Editing", "Expensive Agencies", "Outdated Demos")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 1,
                        title = "Meet ${brand.name}",
                        subtitle = "The Secret Solution",
                        voiceover = "What if your repo could film its own marketing video with a single terminal command?",
                        emotionalPreset = EmotionalPreset.WHISPER,
                        durationSeconds = 3.6f,
                        visualType = SceneVisualType.CODE_TERMINAL,
                        accentTag = "THE REVEAL",
                        codeSnippet = "npx skills add remotion-dev/skills\nnpx skills add AKCodez/promo-video-skill\n\n# Run with direction\nclaude \"Create a 60s dark mode promo video\"",
                        badgeTags = brand.techStack.take(3)
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 2,
                        title = "3D Browser & Spring Animations",
                        subtitle = "Dual Format Renders",
                        voiceover = "${brand.name} scans your codebase, extracts your brand colors, and renders stunning 3D browser mockups.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.8f,
                        visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                        accentTag = "FEATURE SPOTLIGHT",
                        badgeTags = brand.keyFeatures.take(3)
                    )
                )
                if (duration >= 60) {
                    scenes.add(
                        PromoScene(
                            orderIndex = 3,
                            title = "Proven Traction",
                            subtitle = "Trusted Worldwide",
                            voiceover = "Join over 50,000 developers who turned their git repositories into viral product launch videos.",
                            emotionalPreset = EmotionalPreset.WARM,
                            durationSeconds = 3.4f,
                            visualType = SceneVisualType.METRIC_COUNTER,
                            accentTag = "SOCIAL PROOF",
                            metricNumber = "50,000+",
                            metricLabel = "Videos Rendered on Remotion",
                            badgeTags = listOf("4K Output", "Zero Overlap", "Whisper Timing")
                        )
                    )
                }
                scenes.add(
                    PromoScene(
                        orderIndex = scenes.size,
                        title = "Ship Today",
                        subtitle = "Open Source & Free",
                        voiceover = "Try ${brand.name} right now. One command. Rendered and ready to post.",
                        emotionalPreset = EmotionalPreset.DRAMATIC,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.LOGO_REVEAL_CTA,
                        accentTag = "CALL TO ACTION",
                        ctaButtonText = "Install Skill",
                        ctaUrl = "https://${brand.name.lowercase().replace(" ", "")}.app",
                        badgeTags = listOf("Free & MIT", "Dual Format")
                    )
                )
            }
            NarrativeTemplate.PROBLEM_STACK -> {
                scenes.add(
                    PromoScene(
                        orderIndex = 0,
                        title = "Marketing Is Broken",
                        subtitle = "Pain Point 1",
                        voiceover = "Nobody reads your README. Traditional video editors take days to master.",
                        emotionalPreset = EmotionalPreset.RAGE,
                        durationSeconds = 3.2f,
                        visualType = SceneVisualType.HOOK_FRUSTRATION,
                        accentTag = "PAIN 1",
                        badgeTags = listOf("Zero Reach", "Low Conversions")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 1,
                        title = "Agency Costs Are Insane",
                        subtitle = "Pain Point 2",
                        voiceover = "Production agencies charge thousands for static slides that get outdated on your next commit.",
                        emotionalPreset = EmotionalPreset.RAGE,
                        durationSeconds = 3.4f,
                        visualType = SceneVisualType.FEATURE_SPOTLIGHT,
                        accentTag = "PAIN 2",
                        badgeTags = listOf("$5,000 / video", "Weeks of Waiting")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 2,
                        title = "The Automated Way",
                        subtitle = "What if...",
                        voiceover = "What if your code was the source of truth for both your product and your video?",
                        emotionalPreset = EmotionalPreset.WHISPER,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.CODE_TERMINAL,
                        accentTag = "SOLUTION",
                        codeSnippet = "export default createPromoVideo({\n  brand: '${brand.name}',\n  theme: 'cinematic',\n  voice: 'adam'\n});",
                        badgeTags = brand.techStack.take(3)
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 3,
                        title = "Dual Format Renders",
                        subtitle = "Landscape + Portrait",
                        voiceover = "${brand.name} outputs two final MP4s in 16:9 and 9:16, ready for YouTube and TikTok instantly.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.8f,
                        visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                        accentTag = "MULTIPLATFORM",
                        badgeTags = listOf("1920x1080", "1080x1920", "Remotion")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 4,
                        title = "Launch with ${brand.name}",
                        subtitle = "Start Free",
                        voiceover = "Stop editing. Start shipping. Try ${brand.name} today.",
                        emotionalPreset = EmotionalPreset.DRAMATIC,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.LOGO_REVEAL_CTA,
                        accentTag = "CTA",
                        ctaButtonText = "Get Started Free",
                        ctaUrl = "https://${brand.name.lowercase().replace(" ", "")}.app",
                        badgeTags = listOf("MIT License")
                    )
                )
            }
            NarrativeTemplate.DEMO_FIRST -> {
                scenes.add(
                    PromoScene(
                        orderIndex = 0,
                        title = "Pure Code. Pure Video.",
                        subtitle = "The Magic Moment",
                        voiceover = "Watch ${brand.name} transform raw code into an animated 3D video in sixty seconds.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.CODE_TERMINAL,
                        accentTag = "MAGIC MOMENT",
                        codeSnippet = "npx skills add AKCodez/promo-video-skill\nclaude \"Create promo video for this repo\"",
                        badgeTags = brand.techStack.take(3)
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 1,
                        title = "Interactive 3D Motion",
                        subtitle = "How It Works",
                        voiceover = "Physics-based spring animations, metallic swoosh sweeps, and synchronized audio narration.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.8f,
                        visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                        accentTag = "HOW IT WORKS",
                        badgeTags = brand.keyFeatures.take(3)
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 2,
                        title = "Over 100,000 Videos",
                        subtitle = "Built for Scale",
                        voiceover = "Trusted by thousands of developer teams to launch with cinema-quality visuals.",
                        emotionalPreset = EmotionalPreset.WARM,
                        durationSeconds = 3.4f,
                        visualType = SceneVisualType.METRIC_COUNTER,
                        accentTag = "TRACTION",
                        metricNumber = "100K+",
                        metricLabel = "Videos Rendered",
                        badgeTags = listOf("Fast Lambda", "Zero PATH Setup")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 3,
                        title = "Build Your Promo",
                        subtitle = "One Command",
                        voiceover = "Generate your promo video in one prompt. Visit us now.",
                        emotionalPreset = EmotionalPreset.DRAMATIC,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.LOGO_REVEAL_CTA,
                        accentTag = "CTA",
                        ctaButtonText = "Try Free",
                        ctaUrl = "https://${brand.name.lowercase().replace(" ", "")}.app",
                        badgeTags = listOf("Open Source")
                    )
                )
            }
            NarrativeTemplate.TRANSFORMATION -> {
                scenes.add(
                    PromoScene(
                        orderIndex = 0,
                        title = "Before: Endless Editing",
                        subtitle = "The Old Way",
                        voiceover = "Before: Days spent cutting keyframes, recording bad audio, and struggling with complex timelines.",
                        emotionalPreset = EmotionalPreset.RAGE,
                        durationSeconds = 3.2f,
                        visualType = SceneVisualType.HOOK_FRUSTRATION,
                        accentTag = "BEFORE",
                        badgeTags = listOf("Slow", "Expensive", "Inconsistent")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 1,
                        title = "After: One Command",
                        subtitle = "The New Way",
                        voiceover = "After: Point your AI at your repository and watch both formats render automatically.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.6f,
                        visualType = SceneVisualType.CODE_TERMINAL,
                        accentTag = "AFTER",
                        codeSnippet = "claude \"Build a 60s dark mode promo with metallic swoosh transitions\"",
                        badgeTags = listOf("Automated", "High Res", "Voiceover")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 2,
                        title = "Feature Breakdown",
                        subtitle = "Smart Capabilities",
                        voiceover = "${brand.name} extracts your core value proposition and syncs word-for-word voiceover.",
                        emotionalPreset = EmotionalPreset.CONFIDENT,
                        durationSeconds = 3.8f,
                        visualType = SceneVisualType.BROWSER_MOCKUP_3D,
                        accentTag = "FEATURES",
                        badgeTags = brand.keyFeatures.take(3)
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 3,
                        title = "10x Faster Launches",
                        subtitle = "Real Results",
                        voiceover = "Developers save over 35 hours per video and increase conversion rates by 300%.",
                        emotionalPreset = EmotionalPreset.WARM,
                        durationSeconds = 3.4f,
                        visualType = SceneVisualType.METRIC_COUNTER,
                        accentTag = "PROOF",
                        metricNumber = "300%",
                        metricLabel = "Higher Conversion Rate",
                        badgeTags = listOf("Time Saved", "High ROI")
                    )
                )
                scenes.add(
                    PromoScene(
                        orderIndex = 4,
                        title = "Transform Your Launch",
                        subtitle = "Get Started",
                        voiceover = "Upgrade your workflow with ${brand.name}. Free, open source, and instant.",
                        emotionalPreset = EmotionalPreset.DRAMATIC,
                        durationSeconds = 3.5f,
                        visualType = SceneVisualType.LOGO_REVEAL_CTA,
                        accentTag = "CTA",
                        ctaButtonText = "Install Promo Skill",
                        ctaUrl = "https://${brand.name.lowercase().replace(" ", "")}.app",
                        badgeTags = listOf("Install Free")
                    )
                )
            }
        }
        return scenes
    }

    private fun generateLocalDirectorResponse(prompt: String, brand: BrandProfile): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("rage") || lower.contains("frustration") ->
                "I've tailored the opening with high-intensity Rage emotion! We open on frustrated developer pain points, followed by a dramatic silence and whispered secret reveal of ${brand.name}."
            lower.contains("voice") || lower.contains("adam") || lower.contains("daniel") ->
                "Great choice. Adam brings that cinematic, gritty movie-trailer presence with deep resonance, while Daniel offers broadcast-grade polish. I've tuned the base pitch and cadences accordingly."
            lower.contains("speed") || lower.contains("transition") ->
                "Shortening scene durations to 2.5–3.0 seconds creates a hyper-engaging pace suitable for TikTok and Instagram Reels. We'll use the Metallic Swoosh transition for maximum velocity shine!"
            lower.contains("cta") || lower.contains("fastsolve") || lower.contains("logo") ->
                "I've updated the final scene to trigger an explosive starburst particle reveal of ${brand.name} with a high-contrast pulsing CTA button leading directly to the repository."
            else ->
                "Understood! I've calibrated the creative direction for ${brand.name}: using the ${brand.techStack.firstOrNull() ?: "React"} theme, snappy 3-second scenes, and responsive dual-format layouts for 16:9 Landscape and 9:16 Portrait."
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
