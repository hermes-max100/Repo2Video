package com.example.service

import android.util.Log
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Renders promo scenes on the IONOS VPS via the OpenMontage render service
 * (Remotion + ffmpeg + Piper TTS — free path, no paid keys, no Veo).
 *
 * This REPLACES the dead Veo 3.1 fast-preview call (retired Oct 22, 2026) and
 * the synthetic MP4 writer that used to fabricate video bytes on-device.
 * A render either returns a real rendered MP4 from the VPS or fails with an
 * honest error. It never synthesizes bytes and never falls back to the Google
 * sample video.
 */
interface OpenMontageRenderService {

    data class RenderJobResult(
        val jobId: String,
        val durationSeconds: Double,
        val fps: Int,
        val resolution: String,
        val frames: Int,
        val hasAudio: Boolean,
        val sizeBytes: Long
    )

    /**
     * Renders [brand] + [scenes] on the VPS and writes the finished MP4 to
     * [outputFile]. Returns success with the job result, or failure with the
     * render service's error text. Only 16:9 is supported — the checked-in
     * Explainer composition is 1920x1080; other aspects fail honestly.
     */
    suspend fun renderPromo(
        brand: BrandProfile,
        scenes: List<PromoScene>,
        aspectRatio: AspectRatioFormat,
        outputFile: File,
        onProgress: (percent: Int, stage: String) -> Unit = { _, _ -> }
    ): Result<RenderJobResult>
}

/**
 * HTTP implementation of [OpenMontageRenderService] against the render
 * service on the IONOS VPS (Tailscale address, not public internet).
 */
class HttpOpenMontageRenderService(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient()
) : OpenMontageRenderService {

    companion object {
        const val DEFAULT_BASE_URL = "http://100.83.164.96:8891"
        private const val TAG = "OpenMontageRender"
        private const val POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 300 // 15 minutes at 3s intervals
        private val JSON = "application/json".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun renderPromo(
        brand: BrandProfile,
        scenes: List<PromoScene>,
        aspectRatio: AspectRatioFormat,
        outputFile: File,
        onProgress: (percent: Int, stage: String) -> Unit
    ): Result<OpenMontageRenderService.RenderJobResult> = withContext(Dispatchers.IO) {
        if (aspectRatio != AspectRatioFormat.LANDSCAPE_16_9) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "Aspect ${aspectRatio.displayName} is not supported: the VPS " +
                        "Explainer composition renders 1920x1080 only. A vertical " +
                        "Explainer variant has not been built yet."
                )
            )
        }
        if (scenes.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("No scenes to render.")
            )
        }

        try {
            onProgress(10, "Submitting render job to the OpenMontage VPS...")
            val jobId = submitJob(brand, scenes)
            onProgress(20, "Render job $jobId queued on the VPS...")

            var statusJson: JSONObject? = null
            for (attempt in 1..MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                val poll = pollJob(jobId)
                if (poll.isFailure) {
                    return@withContext Result.failure(
                        poll.exceptionOrNull() ?: IllegalStateException("Render poll failed")
                    )
                }
                statusJson = poll.getOrThrow()
                when (statusJson.optString("status")) {
                    "done" -> break
                    "failed" -> return@withContext Result.failure(
                        IllegalStateException(
                            "OpenMontage render failed: " +
                                statusJson.optString("error", "unknown error")
                        )
                    )
                    else -> onProgress(
                        20 + (attempt * 60 / MAX_POLL_ATTEMPTS).coerceAtMost(60),
                        "Rendering on the VPS (job $jobId, ${statusJson.optString("status")})..."
                    )
                }
            }

            val done = statusJson ?: return@withContext Result.failure(
                IllegalStateException("Render job timed out after 15 minutes.")
            )
            if (done.optString("status") != "done") {
                return@withContext Result.failure(
                    IllegalStateException("Render job timed out after 15 minutes.")
                )
            }

            onProgress(85, "Downloading rendered video from the VPS...")
            downloadJob(jobId, outputFile)
            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext Result.failure(
                    IllegalStateException("Render finished but the downloaded file is empty.")
                )
            }

            Result.success(
                OpenMontageRenderService.RenderJobResult(
                    jobId = done.optString("job_id", ""),
                    durationSeconds = done.optDouble("duration_seconds", 0.0),
                    fps = done.optInt("fps", 30),
                    resolution = done.optString("resolution", "1920x1080"),
                    frames = done.optInt("frames", 0),
                    hasAudio = done.optBoolean("has_audio", false),
                    sizeBytes = outputFile.length()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "OpenMontage render failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun submitJob(brand: BrandProfile, scenes: List<PromoScene>): String {
        val sceneArray = JSONArray()
        for (scene in scenes) {
            val kind = when (scene.visualType) {
                SceneVisualType.HOOK_FRUSTRATION -> "hero"
                SceneVisualType.METRIC_COUNTER -> "metric"
                SceneVisualType.LOGO_REVEAL_CTA -> "cta"
                SceneVisualType.CODE_TERMINAL -> "text"
                else -> "feature"
            }
            // A metric scene without a verified number must fail here, at the
            // boundary — the service never invents a number, and neither do we.
            if (kind == "metric" && scene.metricNumber.isNullOrBlank()) {
                throw IllegalArgumentException(
                    "Scene '${scene.title}' has no verified metric number — " +
                        "refusing to render an invented metric."
                )
            }
            val obj = JSONObject().apply {
                put("title", scene.title)
                put("subtitle", scene.subtitle)
                put("voiceover", scene.voiceover)
                put("durationSeconds", scene.durationSeconds.toDouble())
                put("kind", kind)
                if (kind == "metric") {
                    put("stat", scene.metricNumber)
                }
            }
            sceneArray.put(obj)
        }

        val body = JSONObject().apply {
            put("brand", JSONObject().apply {
                put("name", brand.name)
                put("tagline", brand.tagline)
                put("primaryColor", brand.primaryColorHex)
                put("secondaryColor", brand.secondaryColorHex)
            })
            put("scenes", sceneArray)
            put("aspect", "16x9")
            put("voice", "piper")
        }.toString().toRequestBody(JSON)

        val request = Request.Builder().url("$baseUrl/render").post(body).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Render service rejected the job (HTTP ${response.code}): $text"
                )
            }
            return JSONObject(text).optString("job_id").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Render service returned no job id: $text")
        }
    }

    private fun pollJob(jobId: String): Result<JSONObject> {
        return try {
            val request = Request.Builder().url("$baseUrl/render/$jobId").get().build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Result.failure(
                        IllegalStateException(
                            "Render status check failed (HTTP ${response.code}): $text"
                        )
                    )
                } else {
                    Result.success(JSONObject(text))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadJob(jobId: String, outputFile: File) {
        val request = Request.Builder().url("$baseUrl/render/$jobId/download").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Render download failed (HTTP ${response.code})"
                )
            }
            val body = response.body ?: throw IllegalStateException("Empty render download")
            outputFile.outputStream().use { out ->
                body.byteStream().copyTo(out)
            }
        }
    }
}
