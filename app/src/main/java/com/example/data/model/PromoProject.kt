package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "promo_projects")
data class PromoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val tagline: String,
    val repoUrl: String,
    val brandName: String,
    val primaryColorHex: String = "#6366F1",
    val secondaryColorHex: String = "#8B5CF6",
    val accentColorHex: String = "#F59E0B",
    val techStackJson: String = "[\"TypeScript\",\"React\",\"Remotion\"]",
    val featuresJson: String = "[\"One command generation\",\"16:9 & 9:16\"]",
    val narrativeTemplate: String = NarrativeTemplate.RAGE_HOOK.name,
    val targetDurationSeconds: Int = 30,
    val voiceActor: String = VoiceActor.MATILDA.name,
    val musicTrack: String = MusicTrackOption.INSPIRED_AMBIENT.name,
    val transitionStyle: String = TransitionStyle.METALLIC_SWOOSH.name,
    val scenesJson: String = "[]",
    val activeAspectRatio: String = AspectRatioFormat.LANDSCAPE_16_9.name,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val lastSyncAttemptAt: Long? = null,
    val syncRetryCount: Int = 0,
    val syncConflictOutcome: String? = null,
    val scanManifestJson: String = "{}",
    val creativeBriefJson: String = "{}",
    val claimEvidencesJson: String = "[]",
    val licensingMetadataJson: String = "[]",
    val rawSourceDeleted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
