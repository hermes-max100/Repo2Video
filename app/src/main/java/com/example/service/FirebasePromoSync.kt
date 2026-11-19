package com.example.service

import android.content.Context
import com.example.R
import com.example.data.model.CloudPromoProject
import com.example.data.model.PromoProject
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Authenticated Cloud Firestore repository scoped to the provisioned named
 * database (`R.string.firestore_database_id`) under `/users/{userId}/projects/{projectId}`.
 */
class FirebasePromoSync(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val validIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    val isUserSignedIn: Boolean
        get() = try {
            !auth.currentUser?.uid.isNullOrBlank()
        } catch (_: Exception) {
            false
        }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun validateDocId(id: String): String {
        require(id.isNotBlank() && id.length <= 128 && validIdRegex.matches(id)) {
            "Invalid Firestore document ID: $id"
        }
        return id
    }

    /**
     * Upserts the signed-in user's profile document at `/users/{uid}`.
     */
    suspend fun upsertUserProfile(
        displayName: String,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = validateDocId(requireUserId())
            val safeDisplayName = displayName.trim().ifBlank { "DevDirector User" }.take(120)
            val safeEmail = email?.trim()?.takeIf { it.isNotBlank() }?.take(254)
            val safePhoto = photoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1024)

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val createPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeDisplayName)
                    if (safeEmail != null) put("email", safeEmail)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = buildMap<String, Any> {
                    put("displayName", safeDisplayName)
                    if (safeEmail != null) put("email", safeEmail)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Real-time snapshot listener for the signed-in user's cloud promo projects.
     * Uses `ServerTimestampBehavior.ESTIMATE` so pending local writes never resolve null.
     */
    fun observeCloudProjects(userId: String): Flow<List<CloudPromoProject>> = flow {
        val uid = validateDocId(userId)
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("projects")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudPromoProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Fetches the authenticated user's cloud projects once.
     */
    suspend fun fetchCloudProjects(): Result<List<CloudPromoProject>> {
        var colPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = validateDocId(requireUserId())
            colPath = "users/$uid/projects"
            val snapshot = db.collection("users")
                .document(uid)
                .collection("projects")
                .get()
                .await()
            val list = snapshot.toObjects(
                CloudPromoProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colPath)
            Result.failure(e)
        }
    }

    /**
     * Creates or updates a project document under `/users/{uid}/projects/proj_{localId}`.
     * Preserves immutable `userId` and `createdAt` on updates, and omits `@DocumentId` `"id"`.
     */
    suspend fun backupProjectToCloud(project: PromoProject): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        var opType = OperationType.WRITE
        return try {
            val uid = validateDocId(requireUserId())
            val safeLocalId = project.id.coerceAtLeast(0L)
            val docId = validateDocId("proj_$safeLocalId")
            val docRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(docId)
            docPath = docRef.path

            val safeTitle = project.title.trim().ifBlank { "Untitled Promo" }.take(200)
            val safeTagline = project.tagline.trim().take(500)
            val safeRepoUrl = project.repoUrl.trim().take(500)
            val safeBrand = project.brandName.trim().ifBlank { "DevDirector" }.take(120)
            val safePrimary = normalizeHex(project.primaryColorHex, "#6366F1")
            val safeSecondary = normalizeHex(project.secondaryColorHex, "#8B5CF6")
            val safeAccent = normalizeHex(project.accentColorHex, "#F59E0B")
            val safeTemplate = project.narrativeTemplate.trim().ifBlank { "RAGE_HOOK" }.take(64)
            val safeDuration = project.targetDurationSeconds.toLong().coerceIn(5L, 600L)
            val safeVoice = project.voiceActor.trim().ifBlank { "MATILDA" }.take(64)
            val safeMusic = project.musicTrack.trim().ifBlank { "INSPIRED_AMBIENT" }.take(64)
            val safeTransition = project.transitionStyle.trim().ifBlank { "METALLIC_SWOOSH" }.take(64)
            val safeScenesJson = project.scenesJson.take(200_000)
            val safeAspect = project.activeAspectRatio.trim().ifBlank { "LANDSCAPE_16_9" }.take(64)

            val existingSnapshot = docRef.get().await()
            if (!existingSnapshot.exists()) {
                opType = OperationType.CREATE
                val createPayload = mapOf<String, Any>(
                    "userId" to uid,
                    "localProjectId" to safeLocalId,
                    "title" to safeTitle,
                    "tagline" to safeTagline,
                    "repoUrl" to safeRepoUrl,
                    "brandName" to safeBrand,
                    "primaryColorHex" to safePrimary,
                    "secondaryColorHex" to safeSecondary,
                    "accentColorHex" to safeAccent,
                    "narrativeTemplate" to safeTemplate,
                    "targetDurationSeconds" to safeDuration,
                    "voiceActor" to safeVoice,
                    "musicTrack" to safeMusic,
                    "transitionStyle" to safeTransition,
                    "scenesJson" to safeScenesJson,
                    "activeAspectRatio" to safeAspect,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                opType = OperationType.UPDATE
                val updatePayload = mapOf<String, Any>(
                    "localProjectId" to safeLocalId,
                    "title" to safeTitle,
                    "tagline" to safeTagline,
                    "repoUrl" to safeRepoUrl,
                    "brandName" to safeBrand,
                    "primaryColorHex" to safePrimary,
                    "secondaryColorHex" to safeSecondary,
                    "accentColorHex" to safeAccent,
                    "narrativeTemplate" to safeTemplate,
                    "targetDurationSeconds" to safeDuration,
                    "voiceActor" to safeVoice,
                    "musicTrack" to safeMusic,
                    "transitionStyle" to safeTransition,
                    "scenesJson" to safeScenesJson,
                    "activeAspectRatio" to safeAspect,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, docPath)
            Result.failure(e)
        }
    }

    /**
     * Deletes a cloud-synced project owned by the signed-in user.
     */
    suspend fun deleteCloudProject(projectDocId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectDocId"
        return try {
            val uid = validateDocId(requireUserId())
            val safeId = validateDocId(projectDocId)
            val docRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun normalizeHex(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        return if (trimmed.length in 4..16) trimmed else fallback
    }
}
