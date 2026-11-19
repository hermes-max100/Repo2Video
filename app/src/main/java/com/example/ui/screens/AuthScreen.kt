package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.immersiveGradientBackground
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "Auth"

/**
 * Full-screen Google Sign-In authentication gate.
 * Attempts silent auto sign-in via GetGoogleIdOption on launch and always exposes
 * a visible interactive "Sign in with Google" button backed by GetSignInWithGoogleOption.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Silent auto sign-in on startup (single-option GetGoogleIdOption request)
    LaunchedEffect(activity) {
        if (Firebase.auth.currentUser != null) {
            isCheckingSession = false
            onAuthSuccess()
            return@LaunchedEffect
        }
        val act = activity
        val clientId = try {
            context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

        if (act != null && clientId != null) {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val credential = credentialManager.getCredential(act, request).credential
                if (credential is CustomCredential &&
                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    isCheckingSession = false
                    onAuthSuccess()
                    return@LaunchedEffect
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(AUTH_TAG, "Silent auto sign-in unavailable; awaiting interactive sign-in: ${e.message}")
            }
        }
        isCheckingSession = false
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .immersiveGradientBackground(themeMode = AppThemeMode.STUDIO_DARK)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Studio brand emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(listOf(PrimaryIndigo, SecondaryViolet))
                        )
                        .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "DevDirector logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DevDirector Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sign in with Google to unlock Gemini 3.8 Flash direction and Cloud Firestore project sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Feature highlights card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = GlassBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AuthFeatureRow(
                            icon = Icons.Default.AutoAwesome,
                            iconTint = PrimaryIndigo,
                            title = "Firebase AI Logic (Gemini 3.8 Flash)",
                            subtitle = "AI brand discovery, structured scene scripting & live Director Chat"
                        )
                        AuthFeatureRow(
                            icon = Icons.Default.CloudDone,
                            iconTint = AccentEmerald,
                            title = "Cloud Firestore Backup",
                            subtitle = "Private per-user project sync across Android sessions"
                        )
                        AuthFeatureRow(
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFF38BDF8),
                            title = "App Check & Zero-Trust Isolation",
                            subtitle = "Hardware-attested AI calls and owner-only cloud storage"
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x26EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Authentication error",
                                tint = AccentRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                // Primary interactive Google Sign-In button (GetSignInWithGoogleOption)
                Button(
                    onClick = {
                        val act = activity
                        if (act == null) {
                            errorMessage = "Unable to access Activity context for Google Sign-In."
                            return@Button
                        }
                        val clientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (_: Exception) {
                            errorMessage = "Google Sign-In configuration missing: default_web_client_id not found"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val signInOption = GetSignInWithGoogleOption.Builder(
                                    serverClientId = clientId
                                ).build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(signInOption)
                                    .build()
                                val credential = credentialManager.getCredential(act, request).credential
                                if (credential is CustomCredential &&
                                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    Firebase.auth.signInWithCredential(
                                        GoogleAuthProvider.getCredential(token, null)
                                    ).await()
                                    isLoading = false
                                    onAuthSuccess()
                                } else {
                                    isLoading = false
                                    errorMessage = "Unexpected credential type returned by Google Sign-In."
                                }
                            } catch (e: GetCredentialCancellationException) {
                                Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                                isLoading = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e(AUTH_TAG, "Google Sign-In failed", e)
                                isLoading = false
                                errorMessage = e.localizedMessage ?: "Sign in failed. Please try again."
                            }
                        }
                    },
                    enabled = !isLoading && !isCheckingSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937),
                        disabledContainerColor = Color(0xCCFFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading || isCheckingSession) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PrimaryIndigo,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCheckingSession) "Checking session..." else "Signing in...",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    } else {
                        GoogleGMark()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }

                Text(
                    text = "Secured by Firebase Authentication & Credential Manager",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun GoogleGMark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFFF3F4F6)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0xFF4285F4), radius = size.width / 2f)
            drawCircle(color = Color.White, radius = size.width / 3.2f)
            drawRect(
                color = Color(0xFF4285F4),
                topLeft = Offset(center.x - 1f, center.y - 2.5f),
                size = Size(size.width / 2f, 5f)
            )
        }
    }
}
