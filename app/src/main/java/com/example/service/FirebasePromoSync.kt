package com.example.service

import android.util.Log
import com.example.data.model.PromoProject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebasePromoSync {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val isUserSignedIn: Boolean
        get() = try { auth.currentUser != null } catch (_: Exception) { false }

    suspend fun signInAnonymously(): Boolean {
        return try {
            auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            Log.w("FirebasePromoSync", "Anonymous sign in skipped/unavailable: ${e.message}")
            false
        }
    }

    suspend fun backupProjectToCloud(project: PromoProject): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous_creator"
            val docData = hashMapOf(
                "id" to project.id,
                "title" to project.title,
                "tagline" to project.tagline,
                "repoUrl" to project.repoUrl,
                "brandName" to project.brandName,
                "narrativeTemplate" to project.narrativeTemplate,
                "targetDurationSeconds" to project.targetDurationSeconds,
                "voiceActor" to project.voiceActor,
                "musicTrack" to project.musicTrack,
                "transitionStyle" to project.transitionStyle,
                "scenesJson" to project.scenesJson,
                "activeAspectRatio" to project.activeAspectRatio,
                "updatedAt" to project.updatedAt
            )
            firestore.collection("creators")
                .document(userId)
                .collection("promo_projects")
                .document(project.id.toString())
                .set(docData)
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirebasePromoSync", "Firestore sync skipped: ${e.message}")
            false
        }
    }
}
