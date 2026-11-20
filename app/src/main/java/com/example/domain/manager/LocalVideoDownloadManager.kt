package com.example.domain.manager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.DownloadDestination
import com.example.data.model.PromoScene
import com.example.data.model.VideoDownloadResult
import com.example.data.model.VideoQuality
import com.example.service.HttpOpenMontageRenderService
import com.example.service.OpenMontageRenderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Video Download & Storage Manager for DevDirector.
 *
 * Renders the promo on the IONOS VPS via the OpenMontage render service
 * (Remotion + ffmpeg + Piper TTS — free path) and saves the real rendered
 * MP4 to device local storage (Movies / Gallery or Downloads) using modern
 * Scoped Storage (MediaStore.Video.Media / MediaStore.Downloads) on
 * Android 10+ (API 29+) with zero required permissions, and backward
 * compatibility on Android 9 and lower.
 *
 * A render either produces a real rendered file from the VPS or fails with
 * an honest error. This class never synthesizes video bytes on-device: the
 * old synthesizeMp4File writer (fake ftyp/moov/mdat boxes with a
 * "dummyFrame" payload) was deleted as part of the OpenMontage repair.
 */
class LocalVideoDownloadManager(
    private val context: Context,
    private val renderService: OpenMontageRenderService = HttpOpenMontageRenderService()
) {

    companion object {
        private const val TAG = "VideoDownloadManager"
    }

    /**
     * Downloads and saves the rendered promo video to the user's device storage.
     */
    suspend fun saveVideoToDeviceStorage(
        brand: BrandProfile,
        scenes: List<PromoScene>,
        aspectRatio: AspectRatioFormat,
        quality: VideoQuality = VideoQuality.FHD_1080P,
        destination: DownloadDestination = DownloadDestination.MOVIES,
        customBaseName: String? = null,
        onProgress: (percent: Int, stage: String) -> Unit = { _, _ -> }
    ): VideoDownloadResult = withContext(Dispatchers.IO) {
        try {
            onProgress(5, "Initializing render for ${aspectRatio.displayName}...")

            // 1. Calculate resolution and duration
            val (width, height) = calculateResolution(aspectRatio, quality)
            val cleanBrand = (customBaseName ?: brand.name)
                .lowercase()
                .replace("[^a-z0-9_]+".toRegex(), "_")
                .trim('_')
                .ifBlank { "promo" }
            val timestamp = System.currentTimeMillis()
            val formatSlug = when (aspectRatio) {
                AspectRatioFormat.LANDSCAPE_16_9 -> "16x9_landscape"
                AspectRatioFormat.PORTRAIT_9_16 -> "9x16_portrait"
                AspectRatioFormat.SQUARE_1_1 -> "1x1_square"
            }
            val qualitySlug = quality.name.lowercase()
            val fileName = "${cleanBrand}_${formatSlug}_${qualitySlug}_$timestamp.mp4"

            // 2. Render for real on the OpenMontage VPS (Remotion + Piper).
            // A failure here is reported honestly — no fake file is produced.
            val cacheDir = File(context.cacheDir, "promo_renders").apply { if (!exists()) mkdirs() }
            val tempFile = File(cacheDir, "temp_$fileName")

            val renderResult = renderService.renderPromo(
                brand = brand,
                scenes = scenes,
                aspectRatio = aspectRatio,
                outputFile = tempFile,
                onProgress = { pct, stage -> onProgress(pct, stage) }
            )
            if (renderResult.isFailure) {
                throw IllegalStateException(
                    renderResult.exceptionOrNull()?.message
                        ?: "OpenMontage render failed for an unknown reason."
                )
            }
            val rendered = renderResult.getOrThrow()
            // Trust the renderer's verified output, not the scene estimates.
            val durationSeconds = rendered.durationSeconds.toInt().coerceAtLeast(1)
            val renderedResolution = rendered.resolution.ifBlank { "${width}x${height}" }

            onProgress(75, "Rendered ${rendered.frames} frames on the VPS, saving to device...")
            val fileSizeBytes = tempFile.length()

            onProgress(88, "Writing to device local storage (${destination.displayName})...")

            // 3. Save to Public Device Storage via MediaStore or Filesystem
            val savedLocation = writeToPublicStorage(
                tempFile = tempFile,
                fileName = fileName,
                brand = brand,
                aspectRatio = aspectRatio,
                destination = destination,
                durationSeconds = durationSeconds,
                width = width,
                height = height
            )

            onProgress(100, "Saved to ${savedLocation.displayPath}!")

            VideoDownloadResult(
                success = true,
                fileName = fileName,
                fileUriString = savedLocation.uri.toString(),
                localPath = savedLocation.displayPath,
                fileSizeBytes = fileSizeBytes,
                formattedSize = formatBytes(fileSizeBytes),
                aspectRatio = aspectRatio,
                destinationFolder = destination.displayName,
                durationSeconds = durationSeconds,
                resolution = renderedResolution,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download promo video: ${e.message}", e)
            VideoDownloadResult(
                success = false,
                fileName = "failed_render.mp4",
                fileUriString = "",
                localPath = "",
                fileSizeBytes = 0L,
                formattedSize = "0 MB",
                aspectRatio = aspectRatio,
                destinationFolder = destination.displayName,
                durationSeconds = 0,
                resolution = "0x0",
                errorMessage = e.message ?: "Unknown storage export failure"
            )
        }
    }

    /**
     * Batch downloads all 3 master formats (Landscape 16:9, Portrait 9:16, Square 1:1) in sequence.
     */
    suspend fun saveAllFormatsToDeviceStorage(
        brand: BrandProfile,
        scenes: List<PromoScene>,
        quality: VideoQuality = VideoQuality.FHD_1080P,
        destination: DownloadDestination = DownloadDestination.MOVIES,
        onProgress: (overallPercent: Int, status: String) -> Unit = { _, _ -> }
    ): List<VideoDownloadResult> = withContext(Dispatchers.IO) {
        val formats = listOf(
            AspectRatioFormat.LANDSCAPE_16_9,
            AspectRatioFormat.PORTRAIT_9_16,
            AspectRatioFormat.SQUARE_1_1
        )
        val results = mutableListOf<VideoDownloadResult>()

        for ((index, format) in formats.withIndex()) {
            val basePct = (index * 100) / formats.size
            val res = saveVideoToDeviceStorage(
                brand = brand,
                scenes = scenes,
                aspectRatio = format,
                quality = quality,
                destination = destination,
                onProgress = { subPct, stage ->
                    val overall = basePct + (subPct / formats.size)
                    onProgress(overall, "[${index + 1}/3] ${format.displayName}: $stage")
                }
            )
            results.add(res)
        }

        onProgress(100, "All 3 platform master formats saved to ${destination.displayName}!")
        results
    }

    private data class StorageLocation(
        val uri: Uri,
        val displayPath: String
    )

    private fun writeToPublicStorage(
        tempFile: File,
        fileName: String,
        brand: BrandProfile,
        aspectRatio: AspectRatioFormat,
        destination: DownloadDestination,
        durationSeconds: Int,
        width: Int,
        height: Int
    ): StorageLocation {
        // 1. Always create a durable physical file in app-accessible storage
        val appStorageDir = File(
            context.getExternalFilesDir(destination.directoryType) ?: context.filesDir,
            destination.subFolder
        ).apply { if (!exists()) mkdirs() }

        val physicalFile = File(appStorageDir, fileName)
        tempFile.copyTo(physicalFile, overwrite = true)

        val resolver = context.contentResolver

        // 2. On Android 10+ (API 29+), publish to MediaStore (Movies / Downloads)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentUri = if (destination == DownloadDestination.MOVIES) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    // Try Downloads table, fallback to Video if unsupported
                    try {
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } catch (_: Throwable) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.TITLE, "${brand.name} Promo - ${aspectRatio.displayName}")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${destination.directoryType}/${destination.subFolder}")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)

                    if (destination == DownloadDestination.MOVIES) {
                        put(MediaStore.Video.Media.DURATION, durationSeconds * 1000L)
                        put(MediaStore.Video.Media.WIDTH, width)
                        put(MediaStore.Video.Media.HEIGHT, height)
                        put(MediaStore.Video.Media.ARTIST, brand.name)
                        put(MediaStore.Video.Media.ALBUM, "DevDirector Productions")
                    }
                }

                val itemUri = resolver.insert(contentUri, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)

                    return StorageLocation(uri = itemUri, displayPath = physicalFile.absolutePath)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "MediaStore insert failed, falling back to direct filesystem storage: ${e.message}")
            }
        }

        // 3. Fallback: Direct Public Filesystem Storage
        val publicDir = try {
            Environment.getExternalStoragePublicDirectory(destination.directoryType)
        } catch (_: Throwable) {
            null
        }

        val targetDir = if (publicDir != null) {
            File(publicDir, destination.subFolder).apply { if (!exists()) mkdirs() }
        } else {
            appStorageDir
        }
        val destFile = File(targetDir, fileName)
        if (destFile.absolutePath != physicalFile.absolutePath) {
            try {
                tempFile.copyTo(destFile, overwrite = true)
            } catch (_: Throwable) {}
        }

        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        } catch (_: Throwable) {}

        val finalFile = if (destFile.exists()) destFile else physicalFile
        val fileUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                finalFile
            )
        } catch (_: Throwable) {
            Uri.fromFile(finalFile)
        }

        return StorageLocation(uri = fileUri, displayPath = finalFile.absolutePath)
    }

    private fun calculateResolution(aspectRatio: AspectRatioFormat, quality: VideoQuality): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatioFormat.LANDSCAPE_16_9 -> quality.widthLandscape to quality.heightLandscape
            AspectRatioFormat.PORTRAIT_9_16 -> quality.heightLandscape to quality.widthLandscape
            AspectRatioFormat.SQUARE_1_1 -> quality.heightLandscape to quality.heightLandscape
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0.0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb < 1.0) {
            val kb = bytes.toDouble() / 1024
            String.format("%.1f KB", kb)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    /**
     * Creates an Intent to open and play the video in the user's default video player.
     */
    fun createOpenVideoIntent(result: VideoDownloadResult): Intent {
        val uri = Uri.parse(result.fileUriString)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an Intent to share the downloaded promo video via social apps, messaging, or email.
     */
    fun createShareVideoIntent(result: VideoDownloadResult): Intent {
        val uri = Uri.parse(result.fileUriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DevDirector Promo Video: ${result.fileName}")
            putExtra(Intent.EXTRA_TEXT, "Check out our new promo video created with DevDirector!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Promo Video via...")
    }
}
