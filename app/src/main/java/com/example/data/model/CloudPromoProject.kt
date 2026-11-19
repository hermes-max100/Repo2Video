package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudPromoProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val localProjectId: Long = 0L,
    val title: String = "",
    val tagline: String = "",
    val repoUrl: String = "",
    val brandName: String = "",
    val primaryColorHex: String = "#6366F1",
    val secondaryColorHex: String = "#8B5CF6",
    val accentColorHex: String = "#F59E0B",
    val narrativeTemplate: String = NarrativeTemplate.RAGE_HOOK.name,
    val targetDurationSeconds: Long = 30L,
    val voiceActor: String = VoiceActor.MATILDA.name,
    val musicTrack: String = MusicTrackOption.INSPIRED_AMBIENT.name,
    val transitionStyle: String = TransitionStyle.METALLIC_SWOOSH.name,
    val scenesJson: String = "[]",
    val activeAspectRatio: String = AspectRatioFormat.LANDSCAPE_16_9.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toLocalPromoProject(): PromoProject {
        val safeTemplate = enumValues<NarrativeTemplate>()
            .firstOrNull { it.name == narrativeTemplate }
            ?.name ?: NarrativeTemplate.RAGE_HOOK.name
        val safeVoice = enumValues<VoiceActor>()
            .firstOrNull { it.name == voiceActor }
            ?.name ?: VoiceActor.MATILDA.name
        val safeMusic = enumValues<MusicTrackOption>()
            .firstOrNull { it.name == musicTrack }
            ?.name ?: MusicTrackOption.INSPIRED_AMBIENT.name
        val safeTransition = enumValues<TransitionStyle>()
            .firstOrNull { it.name == transitionStyle }
            ?.name ?: TransitionStyle.METALLIC_SWOOSH.name
        val safeAspect = enumValues<AspectRatioFormat>()
            .firstOrNull { it.name == activeAspectRatio }
            ?.name ?: AspectRatioFormat.LANDSCAPE_16_9.name

        val createdMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()

        return PromoProject(
            id = localProjectId,
            title = title,
            tagline = tagline,
            repoUrl = repoUrl,
            brandName = brandName,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            accentColorHex = accentColorHex,
            narrativeTemplate = safeTemplate,
            targetDurationSeconds = targetDurationSeconds.toInt().coerceIn(5, 600),
            voiceActor = safeVoice,
            musicTrack = safeMusic,
            transitionStyle = safeTransition,
            scenesJson = scenesJson,
            activeAspectRatio = safeAspect,
            syncState = SyncState.SYNCED.name,
            lastSyncAttemptAt = updatedMs,
            createdAt = createdMs,
            updatedAt = updatedMs
        )
    }
}
