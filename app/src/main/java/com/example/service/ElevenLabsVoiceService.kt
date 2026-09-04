package com.example.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.EmotionalPreset
import com.example.data.model.VoiceActor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * High-definition ElevenLabs Neural Voiceover Service.
 * Provides hyper-realistic, human-quality voice synthesis with emotional cadence,
 * dynamic stability control, and local audio caching.
 */
class ElevenLabsVoiceService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "elevenlabs_voice_cache").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Resolves the ElevenLabs API Key from BuildConfig, environment variables,
     * or stored application preferences.
     */
    val apiKey: String
        get() {
            // 1. Try BuildConfig.ELEVENLABS_API_KEY
            val b1 = try { BuildConfig.ELEVENLABS_API_KEY } catch (_: Throwable) { "" }
            if (isUsableKey(b1)) return b1.trim()

            // 2. Try BuildConfig.ELEVEN_LABS_API_KEY
            val b2 = try { BuildConfig.ELEVEN_LABS_API_KEY } catch (_: Throwable) { "" }
            if (isUsableKey(b2)) return b2.trim()

            // 3. Try BuildConfig.XI_API_KEY
            val b3 = try { BuildConfig.XI_API_KEY } catch (_: Throwable) { "" }
            if (isUsableKey(b3)) return b3.trim()

            // 4. Try Environment variables
            val env1 = System.getenv("ELEVENLABS_API_KEY") ?: ""
            if (isUsableKey(env1)) return env1.trim()

            val env2 = System.getenv("ELEVEN_LABS_API_KEY") ?: ""
            if (isUsableKey(env2)) return env2.trim()

            val env3 = System.getenv("XI_API_KEY") ?: ""
            if (isUsableKey(env3)) return env3.trim()

            // 5. Fallback: Check local shared preferences
            val sp = context.getSharedPreferences("elevenlabs_prefs", Context.MODE_PRIVATE)
            val spKey = sp.getString("api_key", "") ?: ""
            if (isUsableKey(spKey)) return spKey.trim()

            return ""
        }

    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    fun saveApiKeyOverride(key: String) {
        val sp = context.getSharedPreferences("elevenlabs_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("api_key", key.trim()).apply()
    }

    private fun isUsableKey(k: String): Boolean {
        return k.isNotBlank() &&
                !k.startsWith("MY_") &&
                k != "null" &&
                k.length >= 10
    }

    /**
     * Synthesizes realistic human speech for the specified script, voice actor,
     * and emotional preset using ElevenLabs TTS.
     * Returns the cached MP3 File and audio duration in milliseconds.
     */
    suspend fun synthesizeSpeech(
        text: String,
        voiceActor: VoiceActor,
        emotionalPreset: EmotionalPreset
    ): Result<AudioSynthesisResult> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text script cannot be blank"))
        }

        val key = apiKey
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("ElevenLabs API Key is not configured. Please add it to AI Studio Secrets."))
        }

        // Cache lookup: SHA-256 of voice + emotion + text
        val cacheKey = hashString("${voiceActor.name}_${emotionalPreset.name}_$trimmed")
        val cachedFile = File(cacheDir, "eleven_${cacheKey}.mp3")

        if (cachedFile.exists() && cachedFile.length() > 512) {
            val durationMs = getAudioDurationMs(cachedFile)
            Log.d("ElevenLabsService", "Cache hit for voice ${voiceActor.voiceName}: ${cachedFile.absolutePath} (${durationMs}ms)")
            return@withContext Result.success(AudioSynthesisResult(cachedFile, durationMs, fromCache = true))
        }

        // Try primary model (eleven_turbo_v2_5 for ultra-fast generation), with fallback to eleven_multilingual_v2
        val modelsToTry = listOf("eleven_turbo_v2_5", "eleven_multilingual_v2", "eleven_monolingual_v1")
        var lastException: Exception? = null

        for (modelId in modelsToTry) {
            try {
                val voiceId = voiceActor.elevenLabsVoiceId
                val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_128"

                val jsonPayload = JSONObject().apply {
                    put("text", trimmed)
                    put("model_id", modelId)
                    put("voice_settings", JSONObject().apply {
                        put("stability", emotionalPreset.elevenLabsStability)
                        put("similarity_boost", emotionalPreset.elevenLabsSimilarityBoost)
                        put("style", emotionalPreset.elevenLabsStyle)
                        put("use_speaker_boost", emotionalPreset.elevenLabsSpeakerBoost)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("xi-api-key", key)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "audio/mpeg")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d("ElevenLabsService", "Calling ElevenLabs TTS for voice ${voiceActor.voiceName} ($voiceId), model $modelId...")
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.mp3")
                        FileOutputStream(tempFile).use { output ->
                            body.byteStream().copyTo(output)
                        }

                        if (tempFile.exists() && tempFile.length() > 512) {
                            if (cachedFile.exists()) cachedFile.delete()
                            tempFile.renameTo(cachedFile)
                            val durationMs = getAudioDurationMs(cachedFile)
                            Log.d("ElevenLabsService", "ElevenLabs synthesis success! Generated ${cachedFile.length()} bytes (${durationMs}ms)")
                            return@withContext Result.success(AudioSynthesisResult(cachedFile, durationMs, fromCache = false))
                        } else {
                            tempFile.delete()
                        }
                    }
                } else {
                    val errBody = response.body?.string() ?: ""
                    Log.w("ElevenLabsService", "ElevenLabs error (${response.code}) with model $modelId: $errBody")
                    lastException = Exception("ElevenLabs API error (${response.code}): $errBody")

                    // If unauthorized/bad key or quota exceeded, stop trying models
                    if (response.code == 401 || response.code == 403 || response.code == 429) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w("ElevenLabsService", "Network exception generating speech with $modelId", e)
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Failed to synthesize speech with ElevenLabs"))
    }

    private fun getAudioDurationMs(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull() ?: 3500L
        } catch (_: Throwable) {
            3500L
        }
    }

    private fun hashString(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            input.hashCode().toString()
        }
    }

    data class AudioSynthesisResult(
        val file: File,
        val durationMs: Long,
        val fromCache: Boolean
    )
}
