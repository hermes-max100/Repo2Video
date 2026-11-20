package com.example.service

import android.util.Log
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.EmotionalPreset
import com.example.data.model.NarrativeTemplate
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Gemini AI service powered by Firebase AI Logic (`Firebase.ai`) and App Check.
 * Requires no client-side API key; uses `gemini-3.8-flash` with structured JSON schemas
 * (`Schema.obj(...)`) and streaming multi-turn Director Chat.
 */
class GeminiPromoService {

    companion object {
        private const val TAG = "GeminiPromoService"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        private const val INFERENCE_TIMEOUT_MS = 60_000L
    }

    private val brandSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Product or repository name"),
                "tagline" to Schema.string(description = "Punchy tagline, max 10 words"),
                "description" to Schema.string(description = "Short 1-2 sentence product description"),
                "primaryColorHex" to Schema.string(description = "Modern vibrant hex color like #6366F1"),
                "secondaryColorHex" to Schema.string(description = "Secondary complementary hex color"),
                "accentColorHex" to Schema.string(description = "Accent highlight hex color"),
                "techStack" to Schema.array(
                    items = Schema.string(),
                    description = "4-6 technologies or frameworks used"
                ),
                "keyFeatures" to Schema.array(
                    items = Schema.string(),
                    description = "3-5 killer features, max 8 words each"
                ),
                "targetAudience" to Schema.string(description = "Primary target audience")
            )
        )
    }

    private val scriptSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "scenes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "orderIndex" to Schema.integer(description = "Zero-based scene order"),
                            "title" to Schema.string(description = "Short punchy scene title"),
                            "subtitle" to Schema.string(description = "Short subtitle"),
                            "voiceover" to Schema.string(description = "Compelling narration line (12-25 words)"),
                            "emotionalPreset" to Schema.enumeration(
                                values = listOf("RAGE", "WHISPER", "CONFIDENT", "WARM", "DRAMATIC"),
                                description = "Emotional vocal tone for this scene"
                            ),
                            "durationSeconds" to Schema.double(description = "Scene duration in seconds (2.5 to 5.0)"),
                            "visualType" to Schema.enumeration(
                                values = listOf(
                                    "HOOK_FRUSTRATION",
                                    "CODE_TERMINAL",
                                    "BROWSER_MOCKUP_3D",
                                    "FEATURE_SPOTLIGHT",
                                    "METRIC_COUNTER",
                                    "LOGO_REVEAL_CTA"
                                ),
                                description = "Visual layout preset"
                            ),
                            "accentTag" to Schema.string(description = "Short uppercase category tag"),
                            "codeSnippet" to Schema.string(description = "Optional code snippet", nullable = true),
                            "metricNumber" to Schema.string(description = "Optional metric figure like 50K+", nullable = true),
                            "metricLabel" to Schema.string(description = "Optional metric caption", nullable = true),
                            "badgeTags" to Schema.array(
                                items = Schema.string(),
                                description = "2-3 short highlight chips"
                            ),
                            "ctaButtonText" to Schema.string(description = "Optional CTA button label", nullable = true),
                            "ctaUrl" to Schema.string(description = "Optional CTA URL", nullable = true)
                        ),
                        optionalProperties = listOf(
                            "codeSnippet",
                            "metricNumber",
                            "metricLabel",
                            "ctaButtonText",
                            "ctaUrl"
                        )
                    ),
                    description = "Ordered list of promotional video scenes"
                )
            )
        )
    }

    private val brandDiscoveryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.3f
                responseMimeType = "application/json"
                responseSchema = brandSchema
            },
            systemInstruction = content {
                text(
                    "You are an expert brand designer and technical marketer for developer tools. " +
                        "Analyze the provided repository URL, codebase metadata, or product description " +
                        "and extract a crisp, cohesive brand profile."
                )
            }
        )
    }

    private val scriptGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.4f
                responseMimeType = "application/json"
                responseSchema = scriptSchema
            },
            systemInstruction = content {
                text(
                    "You are an award-winning promotional video director producing high-converting " +
                        "developer product launch storyboards. Match emotional presets (RAGE, WHISPER, " +
                        "CONFIDENT, WARM, DRAMATIC) and visual types to the narrative arc."
                )
            }
        )
    }

    private val directorChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    "You are Gemini Director inside DevDirector Studio. You help developers craft " +
                        "high-converting promo videos for their codebases: rewriting voiceovers with " +
                        "emotional presets (Rage, Whisper, Confident, Warm, Dramatic), tuning 2-4s " +
                        "scene pacing, adapting 16:9 Landscape & 9:16 Portrait layouts, and recommending " +
                        "transitions (Metallic Swoosh, Spring Wipe, Velocity Slide) and voice actors " +
                        "(Matilda, Rachel, Daniel, Josh, Adam). Keep responses concise, actionable, and punchy."
                )
            }
        )
    }

    /**
     * Phase 1: Brand Discovery from codebase info or repository URL using Gemini structured output.
     */
    suspend fun discoverBrand(repoInput: String): Result<BrandProfile> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                brandDiscoveryModel.generateContent("Analyze this repository / codebase info:\n$repoInput")
            }
            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Gemini returned an empty brand profile."))
            }

            val brandJson = JSONObject(jsonString)
            val techStackList = buildList {
                val arr = brandJson.optJSONArray("techStack")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val featuresList = buildList {
                val arr = brandJson.optJSONArray("keyFeatures")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
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
                    techStack = techStackList.ifEmpty { listOf("Kotlin", "Jetpack Compose") },
                    keyFeatures = featuresList.ifEmpty { listOf("Fast setup", "Modern UI", "Cloud scale") },
                    targetAudience = brandJson.optString("targetAudience", "Developers & creators"),
                    repoPathOrUrl = repoInput.take(120)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Brand discovery timed out", e)
            Result.failure(Exception("Brand discovery timed out after 60 seconds. Please try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI brand discovery failed", e)
            Result.failure(e)
        }
    }

    /**
     * Phase 2 & 3: Generate Scene Script based on Narrative Template & Target Duration.
     */
    suspend fun generatePromoScript(
        brand: BrandProfile,
        template: NarrativeTemplate,
        durationSeconds: Int
    ): Result<List<PromoScene>> = withContext(Dispatchers.IO) {
        try {
            val sceneCount = when {
                durationSeconds <= 30 -> 4
                durationSeconds <= 60 -> 5
                else -> 6
            }
            val prompt = """
                Generate a complete $durationSeconds-second promo storyboard ($sceneCount scenes, 2.5s to 4.5s each).
                Narrative Template: ${template.title} (${template.arc})
                Brand Name: ${brand.name}
                Tagline: ${brand.tagline}
                Description: ${brand.description}
                Key Features: ${brand.keyFeatures.joinToString(", ")}
                Tech Stack: ${brand.techStack.joinToString(", ")}
                Target Audience: ${brand.targetAudience}
                Metric rule: only include metricNumber/metricLabel when the number is verified from the repository (e.g. GitHub stars). Never invent a metric number.
            """.trimIndent()

            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                scriptGeneratorModel.generateContent(prompt)
            }
            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Gemini returned an empty script."))
            }

            val scriptObj = JSONObject(jsonString)
            val scenesArray = scriptObj.getJSONArray("scenes")
            val resultScenes = buildList {
                for (i in 0 until scenesArray.length()) {
                    val sc = scenesArray.getJSONObject(i)
                    val badges = buildList {
                        val bArr = sc.optJSONArray("badgeTags")
                        if (bArr != null) {
                            for (j in 0 until bArr.length()) {
                                val tag = bArr.optString(j).trim()
                                if (tag.isNotEmpty()) add(tag)
                            }
                        }
                    }
                    val emotionRaw = sc.optString("emotionalPreset", "CONFIDENT")
                    val visualRaw = sc.optString("visualType", "BROWSER_MOCKUP_3D")
                    val visualType = enumValues<SceneVisualType>()
                        .firstOrNull { it.name == visualRaw } ?: SceneVisualType.BROWSER_MOCKUP_3D
                    // A metric is only shown when the brand carries a verified
                    // number. The model may invent one — never pass its number
                    // through unchecked, and only on METRIC_COUNTER scenes.
                    val isMetricScene = visualType == SceneVisualType.METRIC_COUNTER
                    val metricNumber = if (isMetricScene) brand.verifiedMetricNumber else null
                    val metricLabel = if (isMetricScene) brand.verifiedMetricLabel else null

                    add(
                        PromoScene(
                            id = UUID.randomUUID().toString(),
                            orderIndex = i,
                            title = sc.optString("title", "Scene ${i + 1}"),
                            subtitle = sc.optString("subtitle", ""),
                            voiceover = sc.optString("voiceover", ""),
                            emotionalPreset = enumValues<EmotionalPreset>()
                                .firstOrNull { it.name == emotionRaw } ?: EmotionalPreset.CONFIDENT,
                            durationSeconds = sc.optDouble("durationSeconds", 3.5).toFloat().coerceIn(2.0f, 8.0f),
                            visualType = visualType,
                            accentTag = sc.optString("accentTag", "FEATURE"),
                            codeSnippet = sc.optString("codeSnippet", "").takeIf { it.isNotBlank() && it != "null" },
                            metricNumber = metricNumber,
                            metricLabel = metricLabel,
                            badgeTags = badges,
                            ctaButtonText = sc.optString("ctaButtonText", "").takeIf { it.isNotBlank() && it != "null" },
                            ctaUrl = sc.optString("ctaUrl", "").takeIf { it.isNotBlank() && it != "null" }
                        )
                    )
                }
            }

            if (resultScenes.isEmpty()) {
                Result.failure(IllegalStateException("Gemini generated zero scenes."))
            } else {
                Result.success(resultScenes)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Script generation timed out", e)
            Result.failure(Exception("Script generation timed out after 60 seconds. Please try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI script generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * AI Director streaming multi-turn chat via Firebase AI Logic (`generateContentStream` / `sendMessageStream`).
     */
    fun chatWithDirectorStream(
        history: List<Pair<String, String>>,
        userMessage: String,
        currentProject: BrandProfile
    ): Flow<String> = flow {
        val chatHistory = buildList {
            add(
                content("user") {
                    text(
                        "Current project context — Name: ${currentProject.name}, " +
                            "Tagline: ${currentProject.tagline}, " +
                            "Features: ${currentProject.keyFeatures.joinToString("; ")}, " +
                            "Stack: ${currentProject.techStack.joinToString(", ")}."
                    )
                }
            )
            add(
                content("model") {
                    text("Got it — ready to direct the promo video for ${currentProject.name}.")
                }
            )
            history.forEach { (role, msg) ->
                val normalizedRole = if (role == "user") "user" else "model"
                add(content(normalizedRole) { text(msg) })
            }
        }

        val chat = directorChatModel.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        withTimeout(INFERENCE_TIMEOUT_MS) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val delta = chunk.text.orEmpty()
                if (delta.isNotEmpty()) {
                    accumulated.append(delta)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Unary Director Chat helper delegating to Firebase AI Logic.
     */
    suspend fun chatWithDirector(
        history: List<Pair<String, String>>,
        userMessage: String,
        currentProject: BrandProfile
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var finalReply = ""
            chatWithDirectorStream(history, userMessage, currentProject).collect { accumulated ->
                finalReply = accumulated
            }
            if (finalReply.isBlank()) {
                Result.failure(IllegalStateException("Gemini returned an empty response."))
            } else {
                Result.success(finalReply)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Director chat timed out", e)
            Result.failure(Exception("Director response timed out after 60 seconds. Please try again.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI director chat failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a detailed Veo motion-direction shot prompt via Gemini 3.8 Flash.
     */
    suspend fun generateVeoShotDirection(
        prompt: String,
        format: AspectRatioFormat
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val aspectString = if (format == AspectRatioFormat.LANDSCAPE_16_9) "16:9" else "9:16"
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                directorChatModel.generateContent(
                    "Write a concise 1-sentence cinematic camera & motion directive ($aspectString) for: $prompt"
                )
            }
            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Gemini returned an empty motion directive."))
            } else {
                Result.success(text.trim())
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Motion directive generation timed out", e)
            Result.failure(Exception("Motion directive timed out after 60 seconds.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI motion directive failed", e)
            Result.failure(e)
        }
    }
}
