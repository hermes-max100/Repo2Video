package com.example.data.model

import android.os.Environment

enum class VideoQuality(
    val displayName: String,
    val description: String,
    val widthLandscape: Int,
    val heightLandscape: Int,
    val bitRate: Int,
    val targetFps: Int
) {
    FHD_1080P(
        displayName = "1080p Full HD",
        description = "Standard 60fps broadcast master (Recommended)",
        widthLandscape = 1920,
        heightLandscape = 1080,
        bitRate = 8_000_000,
        targetFps = 60
    ),
    UHD_4K(
        displayName = "4K Remotion Master",
        description = "High-bitrate cinematic export for desktop/TV",
        widthLandscape = 3840,
        heightLandscape = 2160,
        bitRate = 18_000_000,
        targetFps = 60
    ),
    HD_720P(
        displayName = "720p Mobile Fast",
        description = "Lightweight file for fast messaging & low bandwidth",
        widthLandscape = 1280,
        heightLandscape = 720,
        bitRate = 3_500_000,
        targetFps = 30
    )
}

enum class DownloadDestination(
    val displayName: String,
    val directoryType: String,
    val subFolder: String,
    val iconName: String
) {
    MOVIES(
        displayName = "Device Gallery & Movies",
        directoryType = Environment.DIRECTORY_MOVIES,
        subFolder = "DevDirector",
        iconName = "VideoLibrary"
    ),
    DOWNLOADS(
        displayName = "Device Downloads Folder",
        directoryType = Environment.DIRECTORY_DOWNLOADS,
        subFolder = "DevDirector",
        iconName = "Download"
    )
}

enum class VideoDownloadStatus {
    IDLE,
    PREPARING,
    RENDERING_FRAMES,
    ENCODING_VIDEO,
    SYNCHRONIZING_AUDIO,
    SAVING_TO_STORAGE,
    COMPLETED,
    FAILED
}

data class VideoDownloadState(
    val status: VideoDownloadStatus = VideoDownloadStatus.IDLE,
    val progressPercent: Int = 0,
    val stageMessage: String = "",
    val activeFileName: String = "",
    val result: VideoDownloadResult? = null,
    val errorMessage: String? = null
)

data class VideoDownloadResult(
    val success: Boolean,
    val fileName: String,
    val fileUriString: String,
    val localPath: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val aspectRatio: AspectRatioFormat,
    val destinationFolder: String,
    val durationSeconds: Int,
    val resolution: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mimeType: String = "video/mp4",
    val errorMessage: String? = null
)
