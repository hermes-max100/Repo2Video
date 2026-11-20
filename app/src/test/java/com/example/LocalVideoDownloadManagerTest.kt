package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.DownloadDestination
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.data.model.VideoQuality
import com.example.domain.manager.LocalVideoDownloadManager
import com.example.service.OpenMontageRenderService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests verifying the download and local device storage functionality:
 * 1. Real renders come from the injected render service — the saved file must
 *    contain exactly the renderer's bytes (provenance), never synthesized
 *    on-device bytes.
 * 2. Unsupported aspects (9:16, 1:1) fail honestly instead of producing a
 *    fake file.
 * 3. A render-service failure propagates as an honest failure result.
 * 4. MediaStore and legacy file system storage support.
 * 5. Multi-format batch saving (16:9 succeeds; 9:16 and 1:1 fail honestly).
 * 6. Share and View intent generation with FileProvider compatibility.
 * 7. Progress reporting across rendering, encoding, and disk persistence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalVideoDownloadManagerTest {

    private lateinit var context: Context
    private lateinit var downloadManager: LocalVideoDownloadManager

    companion object {
        // Marker bytes the fake renderer writes, so tests can prove the saved
        // file came from the renderer and was not synthesized on-device.
        val RENDER_MARKER = "OPENMONTAGE-RENDER-TEST".toByteArray(Charsets.UTF_8)
    }

    /**
     * Stand-in for HttpOpenMontageRenderService. Writes a minimal MP4-shaped
     * payload (ftyp box + marker) to the output file and reports the job
     * result, or fails honestly for unsupported aspects / injected errors.
     */
    private class FakeRenderService(
        private val failAspects: Set<AspectRatioFormat> = emptySet(),
        private val failWith: String? = null
    ) : OpenMontageRenderService {

        override suspend fun renderPromo(
            brand: BrandProfile,
            scenes: List<PromoScene>,
            aspectRatio: AspectRatioFormat,
            outputFile: File,
            onProgress: (percent: Int, stage: String) -> Unit
        ): Result<OpenMontageRenderService.RenderJobResult> {
            if (failWith != null) {
                return Result.failure(IllegalStateException(failWith))
            }
            if (aspectRatio in failAspects) {
                return Result.failure(
                    IllegalArgumentException(
                        "Aspect ${aspectRatio.displayName} is not supported: " +
                            "the VPS Explainer composition renders 1920x1080 only."
                    )
                )
            }
            val header = byteArrayOf(0, 0, 0, 24) + "ftypisom".toByteArray(Charsets.US_ASCII)
            outputFile.writeBytes(header + RENDER_MARKER)
            return Result.success(
                OpenMontageRenderService.RenderJobResult(
                    jobId = "test-job",
                    durationSeconds = scenes.sumOf { it.durationSeconds.toDouble() },
                    fps = 30,
                    resolution = "1920x1080",
                    frames = 900,
                    hasAudio = true,
                    sizeBytes = outputFile.length()
                )
            )
        }
    }

    private val testBrand = BrandProfile(
        name = "DevDirector",
        tagline = "Autonomous Promo Video Studio",
        description = "Deterministic code-to-video studio",
        primaryColorHex = "#6366F1",
        secondaryColorHex = "#8B5CF6",
        accentColorHex = "#06B6D4"
    )

    private val testScenes = listOf(
        PromoScene(
            id = "s1",
            orderIndex = 0,
            title = "Attention Grabber",
            voiceover = "Turn any GitHub repo into a high-converting promotional video in seconds.",
            durationSeconds = 4.0f,
            visualType = SceneVisualType.CODE_TERMINAL,
            codeSnippet = "git clone https://github.com/developer/app.git"
        ),
        PromoScene(
            id = "s2",
            orderIndex = 1,
            title = "Core Architecture",
            voiceover = "Automatic evidence-based claim gates ensure zero hallucinated features.",
            durationSeconds = 4.5f,
            visualType = SceneVisualType.FEATURE_SPOTLIGHT
        ),
        PromoScene(
            id = "s3",
            orderIndex = 2,
            title = "Launch Call to Action",
            voiceover = "Try DevDirector AI today and launch like a funded startup.",
            durationSeconds = 3.5f,
            visualType = SceneVisualType.LOGO_REVEAL_CTA
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        downloadManager = LocalVideoDownloadManager(
            context,
            renderService = FakeRenderService(
                failAspects = setOf(
                    AspectRatioFormat.PORTRAIT_9_16,
                    AspectRatioFormat.SQUARE_1_1
                )
            )
        )
    }

    @Test
    fun testDownloadLandscapeVideoSucceeds() = runBlocking {
        var lastProgress = 0
        val stageList = mutableListOf<String>()

        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.LANDSCAPE_16_9,
            quality = VideoQuality.FHD_1080P,
            destination = DownloadDestination.MOVIES,
            onProgress = { pct, stage ->
                lastProgress = pct
                stageList.add(stage)
            }
        )

        assertTrue("Download should succeed", result.success)
        assertEquals("Progress should reach 100", 100, lastProgress)
        assertTrue("Stage list should be populated", stageList.isNotEmpty())
        assertTrue("File name should include 16x9", result.fileName.contains("16x9_landscape"))
        assertTrue("File size should be greater than zero", result.fileSizeBytes > 0)
        assertEquals("Resolution should be 1920x1080", "1920x1080", result.resolution)
        assertEquals("MIME type should be video/mp4", "video/mp4", result.mimeType)
        assertNotNull("Local path should not be null", result.localPath)

        // Provenance: the saved file must be exactly the renderer's bytes.
        val savedBytes = File(result.localPath).readBytes()
        val markerText = String(savedBytes, Charsets.UTF_8)
        assertTrue(
            "Saved file must contain the renderer's marker bytes",
            markerText.contains("OPENMONTAGE-RENDER-TEST")
        )
        assertFalse(
            "Saved file must not contain the old synthetic watermark",
            markerText.contains("DevDirector Autonomous Remotion Render")
        )
    }

    @Test
    fun testDownloadPortraitVideoFailsHonestly() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.PORTRAIT_9_16,
            quality = VideoQuality.FHD_1080P,
            destination = DownloadDestination.DOWNLOADS
        )

        assertFalse("Portrait download must fail — no vertical renderer exists yet", result.success)
        assertNotNull("Failure must carry an honest error message", result.errorMessage)
        assertTrue(
            "Error must name the unsupported aspect",
            result.errorMessage!!.contains("9:16") || result.errorMessage!!.contains("not supported")
        )
    }

    @Test
    fun testDownloadSquareVideoFailsHonestly() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.SQUARE_1_1,
            quality = VideoQuality.UHD_4K,
            destination = DownloadDestination.MOVIES
        )

        assertFalse("Square download must fail — no square renderer exists yet", result.success)
        assertNotNull("Failure must carry an honest error message", result.errorMessage)
    }

    @Test
    fun testSaveAllFormatsBatch() = runBlocking {
        val progressStages = mutableListOf<String>()
        val results = downloadManager.saveAllFormatsToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            quality = VideoQuality.FHD_1080P,
            destination = DownloadDestination.MOVIES,
            onProgress = { _, stage ->
                progressStages.add(stage)
            }
        )

        assertEquals("Should return all 3 format results", 3, results.size)
        assertTrue("Progress stages should be logged", progressStages.isNotEmpty())

        val byAspect = results.associateBy { it.aspectRatio }
        assertTrue(
            "Landscape 16:9 must succeed",
            byAspect[AspectRatioFormat.LANDSCAPE_16_9]?.success == true
        )
        assertFalse(
            "Portrait 9:16 must fail honestly",
            byAspect[AspectRatioFormat.PORTRAIT_9_16]?.success == true
        )
        assertFalse(
            "Square 1:1 must fail honestly",
            byAspect[AspectRatioFormat.SQUARE_1_1]?.success == true
        )
        assertNotNull(
            "Portrait failure must carry an error message",
            byAspect[AspectRatioFormat.PORTRAIT_9_16]?.errorMessage
        )
    }

    @Test
    fun testRenderFailureReturnsHonestError() = runBlocking {
        val failingManager = LocalVideoDownloadManager(
            context,
            renderService = FakeRenderService(failWith = "Remotion render failed (rc=1): boom")
        )
        val result = failingManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.LANDSCAPE_16_9
        )

        assertFalse("A failed render must not report success", result.success)
        assertNotNull("Failure must carry the renderer's error", result.errorMessage)
        assertTrue(
            "Error must pass through the renderer's message",
            result.errorMessage!!.contains("Remotion render failed")
        )
    }

    @Test
    fun testGeneratedFileIntegrity() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.LANDSCAPE_16_9
        )

        val file = File(result.localPath)
        assertTrue("Output file must exist on disk", file.exists())
        assertTrue("Output file must have non-zero size", file.length() > 0)

        // Validate ISOBMFF / MP4 header bytes (ftyp box)
        val bytes = ByteArray(16)
        file.inputStream().use { it.read(bytes) }
        val ftypString = String(bytes, 4, 4, Charsets.US_ASCII)
        assertEquals("File must contain ftyp box indicator", "ftyp", ftypString)

        // And the bytes must be the renderer's, not synthesized on-device.
        val markerText = String(file.readBytes(), Charsets.UTF_8)
        assertTrue(
            "Saved file must contain the renderer's marker bytes",
            markerText.contains("OPENMONTAGE-RENDER-TEST")
        )
    }

    @Test
    fun testIntentCreation() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.LANDSCAPE_16_9
        )

        val openIntent = downloadManager.createOpenVideoIntent(result)
        assertEquals("Intent action should be ACTION_VIEW", android.content.Intent.ACTION_VIEW, openIntent.action)
        assertEquals("MIME type should be video/mp4", "video/mp4", openIntent.type)
        assertTrue("FLAG_GRANT_READ_URI_PERMISSION must be set", (openIntent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        val shareIntent = downloadManager.createShareVideoIntent(result)
        assertEquals("Share intent action should be ACTION_CHOOSER", android.content.Intent.ACTION_CHOOSER, shareIntent.action)
    }
}
