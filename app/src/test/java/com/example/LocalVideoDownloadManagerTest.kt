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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
 * 1. MP4 Generation with valid H.264/ISOBMFF structure
 * 2. Resolution, bitrates, and aspect ratio calibration
 * 3. MediaStore and legacy file system storage support
 * 4. Multi-format batch saving (16:9, 9:16, 1:1)
 * 5. Share and View intent generation with FileProvider compatibility
 * 6. Progress reporting across rendering, encoding, and disk persistence
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalVideoDownloadManagerTest {

    private lateinit var context: Context
    private lateinit var downloadManager: LocalVideoDownloadManager

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
        downloadManager = LocalVideoDownloadManager(context)
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
        assertTrue("File size should be greater than zero", result.fileSizeBytes > 1000)
        assertEquals("Resolution should be 1920x1080", "1920x1080", result.resolution)
        assertEquals("MIME type should be video/mp4", "video/mp4", result.mimeType)
        assertNotNull("Local path should not be null", result.localPath)
    }

    @Test
    fun testDownloadPortraitVideoSucceeds() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.PORTRAIT_9_16,
            quality = VideoQuality.FHD_1080P,
            destination = DownloadDestination.DOWNLOADS
        )

        assertTrue("Portrait download should succeed", result.success)
        assertTrue("File name should indicate 9x16 portrait", result.fileName.contains("9x16_portrait"))
        assertEquals("Resolution should be 1080x1920", "1080x1920", result.resolution)
        assertTrue("Destination folder should be Download", result.destinationFolder.contains("Download"))
    }

    @Test
    fun testDownloadSquareVideoSucceeds() = runBlocking {
        val result = downloadManager.saveVideoToDeviceStorage(
            brand = testBrand,
            scenes = testScenes,
            aspectRatio = AspectRatioFormat.SQUARE_1_1,
            quality = VideoQuality.UHD_4K,
            destination = DownloadDestination.MOVIES
        )

        assertTrue("Square 4K download should succeed", result.success)
        assertTrue("File name should indicate 1x1 square", result.fileName.contains("1x1_square"))
        assertEquals("Resolution should be 2160x2160", "2160x2160", result.resolution)
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

        assertEquals("Should save all 3 formats", 3, results.size)
        assertTrue("All formats should succeed", results.all { it.success })
        assertTrue("Progress stages should be logged", progressStages.isNotEmpty())

        val aspectRatios = results.map { it.aspectRatio }.toSet()
        assertTrue("Should include 16:9", aspectRatios.contains(AspectRatioFormat.LANDSCAPE_16_9))
        assertTrue("Should include 9:16", aspectRatios.contains(AspectRatioFormat.PORTRAIT_9_16))
        assertTrue("Should include 1:1", aspectRatios.contains(AspectRatioFormat.SQUARE_1_1))
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
