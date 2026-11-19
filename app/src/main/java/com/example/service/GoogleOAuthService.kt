package com.example.service

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleUserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val projectId: String = "matrix-decoded",
    val brandName: String = "DevDirector",
    val scopes: List<String> = listOf(
        "openid",
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
    ),
    val lastSignedInMillis: Long = System.currentTimeMillis()
)

sealed class GoogleAuthState {
    data object Unauthenticated : GoogleAuthState()
    data class Authenticating(val message: String = "Connecting to Google Sign-In...") : GoogleAuthState()
    data class Authenticated(val user: GoogleUserProfile) : GoogleAuthState()
    data class Error(val message: String) : GoogleAuthState()
}

/**
 * Reactive Flow wrapper around FirebaseAuth.AuthStateListener.
 */
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Manages Google Sign-In exclusively via Jetpack Credential Manager and Firebase Authentication.
 * Uses GetSignInWithGoogleOption for interactive sign-in and GetGoogleIdOption in a separate
 * request for silent auto-sign-in (one credential option per request).
 */
class GoogleOAuthService(private val context: Context) {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)
    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.Unauthenticated)
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _authState.value = if (user != null) {
            GoogleAuthState.Authenticated(user.toProfile())
        } else {
            GoogleAuthState.Unauthenticated
        }
    }

    val currentUser: GoogleUserProfile?
        get() = (authState.value as? GoogleAuthState.Authenticated)?.user

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    init {
        auth.currentUser?.let {
            _authState.value = GoogleAuthState.Authenticated(it.toProfile())
        }
        auth.addAuthStateListener(authStateListener)
    }

    fun release() {
        auth.removeAuthStateListener(authStateListener)
    }

    private fun resolveWebClientId(): String? = try {
        appContext.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /**
     * Silent auto sign-in on startup using a single-option GetGoogleIdOption request.
     * Falls back silently to Unauthenticated if no authorized credential is available.
     */
    suspend fun attemptAutoSignIn(activity: Activity): Boolean {
        if (auth.currentUser != null) {
            _authState.value = GoogleAuthState.Authenticated(auth.currentUser!!.toProfile())
            return true
        }
        val clientId = resolveWebClientId() ?: return false
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                auth.currentUser?.let {
                    _authState.value = GoogleAuthState.Authenticated(it.toProfile())
                }
                true
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google auto sign-in unavailable: ${e.message}")
            false
        }
    }

    /**
     * Interactive Google Sign-In flow using exclusively GetSignInWithGoogleOption
     * (one credential option per request for API 36 bottom sheet responsiveness).
     */
    suspend fun signInWithGoogle(activity: Activity): Result<GoogleUserProfile> {
        val clientId = resolveWebClientId()
        if (clientId == null) {
            val msg = "Google Sign-In configuration missing: default_web_client_id not found"
            _authState.value = GoogleAuthState.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        _authState.value = GoogleAuthState.Authenticating("Opening Google Sign-In...")

        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                val firebaseUser = auth.currentUser
                    ?: throw IllegalStateException("Firebase authentication completed without user session.")
                val profile = firebaseUser.toProfile()
                _authState.value = GoogleAuthState.Authenticated(profile)
                Result.success(profile)
            } else {
                val err = "Unexpected credential type: ${credential.type}"
                _authState.value = GoogleAuthState.Error(err)
                Result.failure(IllegalStateException(err))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            _authState.value = GoogleAuthState.Unauthenticated
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Google Sign-In failed"
            _authState.value = GoogleAuthState.Error(msg)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Sign in failed"
            _authState.value = GoogleAuthState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Clears Credential Manager state BEFORE signing out of Firebase inside NonCancellable
     * so active UI unmount does not interrupt teardown or trigger PERMISSION_DENIED races.
     */
    suspend fun signOut(onBeforeFirebaseSignOut: () -> Unit = {}) {
        withContext(NonCancellable) {
            try {
                onBeforeFirebaseSignOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                try {
                    auth.signOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase signOut error: ${e.message}")
                }
                _authState.value = GoogleAuthState.Unauthenticated
            }
        }
    }

    private fun FirebaseUser.toProfile(): GoogleUserProfile = GoogleUserProfile(
        uid = uid,
        displayName = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")
            ?: "DevDirector User",
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString()
    )

    companion object {
        private const val TAG = "Auth"
        const val DEFAULT_PROJECT_ID = "matrix-decoded"
        const val DEFAULT_BRAND_NAME = "DevDirector"
    }
}
