package com.example.service

import android.util.Log
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

class XAiGrokService(private val authService: XAiOAuthService) {

    private val tag = "XAiGrokService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val grokApiUrl = "https://api.x.ai/v1/chat/completions"

    val isAvailable: Boolean
        get() = authService.isConnected

    /**
     * Grok 2 powered Brand Discovery from repository code or product description.
     */
    suspend fun discoverBrand(repoInput: String): Result<BrandProfile> = withContext(Dispatchers.IO) {
        val token = authService.getAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("xAI SuperGrok account not connected"))
        }

        try {
            val systemPrompt = """
                You are Grok, xAI's brilliant, witty, high-energy technical branding strategist.
                Analyze the provided repo description or codebase input and return a modern JSON brand profile:
                {
                  "name": "Product Name",
                  "tagline": "Punchy high-impact tagline (max 10 words)",
                  "description": "Short compelling summary (1-2 sentences)",
                  "primaryColorHex": "#6366F1",
                  "secondaryColorHex": "#8B5CF6",
                  "accentColorHex": "#10B981",
                  "techStack": ["Next.js", "TypeScript", "Tailwind", "Python"],
                  "keyFeatures": ["Lightning fast sync", "Zero config deploys", "Autonomous agents"],
                  "targetAudience": "Founders, developers and creators"
                }
                Respond with valid JSON only.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("model", "grok-2-latest")
                put("temperature", 0.7)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Analyze this project input:\n$repoInput")
                    })
                })
            }

            val request = Request.Builder()
                .url(grokApiUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(tag, "Grok API Error: ${response.code} $body")
                return@withContext Result.failure(Exception("Grok API error ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val cleanJson = cleanJsonResponse(content)
            val brandJson = JSONObject(cleanJson)

            val techStackList = mutableListOf<String>()
            brandJson.optJSONArray("techStack")?.let { arr ->
                for (i in 0 until arr.length()) techStackList.add(arr.getString(i))
            }

            val featuresList = mutableListOf<String>()
            brandJson.optJSONArray("keyFeatures")?.let { arr ->
                for (i in 0 until arr.length()) featuresList.add(arr.getString(i))
            }

            Result.success(
                BrandProfile(
                    name = brandJson.optString("name", "Grok Brand"),
                    tagline = brandJson.optString("tagline", "Engineered for pure speed"),
                    description = brandJson.optString("description", "Next-generation promo production."),
                    logoIcon = "bolt",
                    primaryColorHex = brandJson.optString("primaryColorHex", "#10B981"),
                    secondaryColorHex = brandJson.optString("secondaryColorHex", "#06B6D4"),
                    accentColorHex = brandJson.optString("accentColorHex", "#F59E0B"),
                    techStack = if (techStackList.isNotEmpty()) techStackList else listOf("TypeScript", "Kotlin", "xAI Grok"),
                    keyFeatures = if (featuresList.isNotEmpty()) featuresList else listOf("Ultra-fast AI generation", "Studio audio", "SuperGrok integration"),
                    targetAudience = brandJson.optString("targetAudience", "Modern software builders"),
                    repoPathOrUrl = repoInput.take(60)
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Grok brand discovery failed", e)
            Result.failure(e)
        }
    }

    /**
     * Grok 2 powered high-energy Promo Video Scene script generation.
     */
    suspend fun generatePromoScript(
        brand: BrandProfile,
        template: NarrativeTemplate,
        durationSeconds: Int
    ): Result<List<PromoScene>> = withContext(Dispatchers.IO) {
        val token = authService.getAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("xAI SuperGrok account not connected"))
        }

        try {
            val sceneCount = when (durationSeconds) {
                30 -> 4
                60 -> 5
                else -> 6
            }

            val systemPrompt = """
                You are Grok, an elite promo video director and master copywriter.
                Produce a high-energy, engaging $durationSeconds-second promo video storyboard ($sceneCount scenes) for ${brand.name}.
                Narrative Template: ${template.title} (${template.arc})
                Tagline: ${brand.tagline}
                Features: ${brand.keyFeatures.joinToString(", ")}
                Tech Stack: ${brand.techStack.joinToString(", ")}

                Each scene must have:
                - title (punchy headline)
                - subtitle
                - voiceover (high-impact spoken copy, 12-25 words)
                - emotionalPreset: RAGE, WHISPER, CONFIDENT, WARM, or DRAMATIC
                - durationSeconds: between 2.5 and 4.5
                - visualType: HOOK_FRUSTRATION, CODE_TERMINAL, BROWSER_MOCKUP_3D, FEATURE_SPOTLIGHT, METRIC_COUNTER, or LOGO_REVEAL_CTA
                - accentTag
                - metricNumber & metricLabel (if METRIC_COUNTER)
                - codeSnippet (if CODE_TERMINAL)
                - ctaButtonText & ctaUrl (if LOGO_REVEAL_CTA)

                Format your answer strictly as JSON:
                {
                  "scenes": [
                    {
                      "orderIndex": 0,
                      "title": "Title",
                      "subtitle": "Subtitle",
                      "voiceover": "Voiceover line",
                      "emotionalPreset": "RAGE",
                      "durationSeconds": 3.5,
                      "visualType": "HOOK_FRUSTRATION",
                      "accentTag": "The Hook",
                      "codeSnippet": null,
                      "metricNumber": null,
                      "metricLabel": null,
                      "badgeTags": ["Zero Latency"],
                      "ctaButtonText": null,
                      "ctaUrl": null
                    }
                  ]
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("model", "grok-2-latest")
                put("temperature", 0.7)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Generate $sceneCount scene promo storyboard for ${brand.name}.")
                    })
                })
            }

            val request = Request.Builder()
                .url(grokApiUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(tag, "Grok Script generation error: ${response.code} $body")
                return@withContext Result.failure(Exception("Grok generation error ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val cleanJson = cleanJsonResponse(content)
            val scenesJson = JSONObject(cleanJson).getJSONArray("scenes")
            val scenes = mutableListOf<PromoScene>()

            for (i in 0 until scenesJson.length()) {
                val item = scenesJson.getJSONObject(i)
                val presetStr = item.optString("emotionalPreset", "CONFIDENT")
                val preset = try {
                    EmotionalPreset.valueOf(presetStr.uppercase())
                } catch (_: Exception) {
                    EmotionalPreset.CONFIDENT
                }

                val visualTypeStr = item.optString("visualType", "FEATURE_SPOTLIGHT")
                val visualType = try {
                    SceneVisualType.valueOf(visualTypeStr.uppercase())
                } catch (_: Exception) {
                    SceneVisualType.FEATURE_SPOTLIGHT
                }

                val badges = mutableListOf<String>()
                item.optJSONArray("badgeTags")?.let { arr ->
                    for (b in 0 until arr.length()) badges.add(arr.getString(b))
                }

                scenes.add(
                    PromoScene(
                        orderIndex = item.optInt("orderIndex", i),
                        title = item.optString("title", "Scene ${i + 1}"),
                        subtitle = item.optString("subtitle", brand.name),
                        voiceover = item.optString("voiceover", "Introducing the future of ${brand.name}."),
                        emotionalPreset = preset,
                        durationSeconds = item.optDouble("durationSeconds", 3.5).toFloat(),
                        visualType = visualType,
                        accentTag = item.optString("accentTag", "Scene ${i + 1}"),
                        codeSnippet = if (item.has("codeSnippet") && !item.isNull("codeSnippet")) item.getString("codeSnippet") else null,
                        metricNumber = if (item.has("metricNumber") && !item.isNull("metricNumber")) item.getString("metricNumber") else null,
                        metricLabel = if (item.has("metricLabel") && !item.isNull("metricLabel")) item.getString("metricLabel") else null,
                        badgeTags = badges,
                        ctaButtonText = if (item.has("ctaButtonText") && !item.isNull("ctaButtonText")) item.getString("ctaButtonText") else null,
                        ctaUrl = if (item.has("ctaUrl") && !item.isNull("ctaUrl")) item.getString("ctaUrl") else null
                    )
                )
            }

            Result.success(scenes)
        } catch (e: Exception) {
            Log.e(tag, "Grok Promo script generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Grok 2 interactive chat with the AI Director Terminal.
     */
    suspend fun chatWithDirector(
        history: List<Pair<String, String>>,
        userMessage: String,
        currentProject: BrandProfile
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = authService.getAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("xAI SuperGrok account not connected"))
        }

        try {
            val systemPrompt = """
                You are Grok 2, directing a cinematic promo video for ${currentProject.name}.
                Give punchy, witty, and razor-sharp directorial advice.
                Guide the creator on voiceover pacing, transition hooks, color grading, and narrative punchlines.
                Keep responses under 3 sentences for fast reading.
            """.trimIndent()

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            history.takeLast(6).forEach { (role, content) ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (role == "user") "user" else "assistant")
                    put("content", content)
                })
            }

            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val requestBodyJson = JSONObject().apply {
                put("model", "grok-2-latest")
                put("temperature", 0.7)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(grokApiUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Grok director error ${response.code}"))
            }

            val json = JSONObject(body)
            val reply = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(reply.trim())
        } catch (e: Exception) {
            Log.e(tag, "Grok director chat failed", e)
            Result.failure(e)
        }
    }

    private fun cleanJsonResponse(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean
    }
}
