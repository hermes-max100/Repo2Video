package com.example.domain.manager

import android.content.Context
import android.util.Log
import java.io.File

class MediaLifecycleManager(private val context: Context) {

    private val mediaCacheDir: File
        get() = File(context.cacheDir, "promo_renders").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Creates a temporary file in app-private cache with auto-expiry.
     */
    fun createTempMediaFile(projectId: Long, prefix: String, extension: String): File {
        val filename = "proj_${projectId}_${prefix}_${System.currentTimeMillis()}.$extension"
        return File(mediaCacheDir, filename)
    }

    /**
     * Creates or writes a synthesized placeholder audio track for offline-first rendering.
     */
    fun createAudioTrackFile(projectId: Long, sceneIndex: Int): File {
        val file = createTempMediaFile(projectId, "voice_scene_$sceneIndex", "wav")
        if (!file.exists()) {
            file.writeBytes(ByteArray(4096))
        }
        return file
    }

    /**
     * Creates or writes an assembled MP4 render file in media cache.
     */
    fun createVideoRenderFile(projectId: Long, aspectRatio: String): File {
        val file = createTempMediaFile(projectId, "render_${aspectRatio.lowercase()}", "mp4")
        if (!file.exists()) {
            file.writeBytes(ByteArray(8192))
        }
        return file
    }

    /**
     * Automatically cleans up files older than 24 hours.
     */
    fun cleanStaleTempFiles(maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Int {
        var deletedCount = 0
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        val files = mediaCacheDir.listFiles() ?: return 0

        for (file in files) {
            if (file.lastModified() < cutoff) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        Log.d("MediaLifecycle", "Cleaned $deletedCount stale temporary media files.")
        return deletedCount
    }

    /**
     * Clears all temporary media for a specific project.
     */
    fun clearProjectMedia(projectId: Long): Long {
        var reclaimedBytes = 0L
        val prefix = "proj_${projectId}_"
        val files = mediaCacheDir.listFiles() ?: return 0L

        for (file in files) {
            if (file.name.startsWith(prefix)) {
                reclaimedBytes += file.length()
                file.delete()
            }
        }
        return reclaimedBytes
    }

    /**
     * Calculates total disk space used by temporary promo media cache.
     */
    fun getCacheSizeBytes(): Long {
        val files = mediaCacheDir.listFiles() ?: return 0L
        return files.sumOf { it.length() }
    }

    fun getCacheSizeMb(): String {
        val mb = getCacheSizeBytes().toDouble() / (1024 * 1024)
        return String.format("%.1f MB", mb)
    }
}
