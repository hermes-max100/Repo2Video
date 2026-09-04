package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.GoogleAuthState
import com.example.service.GoogleUserProfile
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PromoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOAuthDialog(
    viewModel: PromoViewModel,
    onDismiss: () -> Unit
) {
    val authState by viewModel.googleAuthState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val activity = context as? Activity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF090A10),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Google Multi-color emblem container
                    GoogleEmblemIcon()
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Google OAuth 2.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "DevDirector • matrix-decoded",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (val state = authState) {
                is GoogleAuthState.Authenticated -> {
                    AuthenticatedView(
                        user = state.user,
                        onSyncProjects = { viewModel.syncCurrentProjectToCloud() },
                        onSignOut = { viewModel.signOutGoogle(activity) }
                    )
                }
                is GoogleAuthState.Authenticating -> {
                    AuthenticatingView(message = state.message)
                }
                is GoogleAuthState.Unauthenticated, is GoogleAuthState.Error -> {
                    val errorMessage = (state as? GoogleAuthState.Error)?.message
                    UnauthenticatedView(
                        errorMessage = errorMessage,
                        onSignIn = {
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity)
                            } else {
                                viewModel.signInWithConfirmedGoogleAccount()
                            }
                        },
                        onQuickConnect = {
                            viewModel.signInWithConfirmedGoogleAccount()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleEmblemIcon() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161822))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            // Draw stylized Google G quad-color segments
            drawCircle(color = Color(0xFF4285F4), radius = size.width / 2f)
            drawCircle(color = Color(0xFF090A10), radius = size.width / 3.2f)
            drawRect(
                color = Color(0xFF4285F4),
                topLeft = Offset(center.x - 2f, center.y - 3.5f),
                size = androidx.compose.ui.geometry.Size(size.width / 2f, 7f)
            )
        }
    }
}

@Composable
private fun AuthenticatedView(
    user: GoogleUserProfile,
    onSyncProjects: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active User Profile Glass Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF10121D))
                .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar / Initial circle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald)
                            )
                            Text(
                                text = "OAuth 2.0 Authenticated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Project & Brand pill strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoPill(label = "Cloud Project", value = user.projectId, modifier = Modifier.weight(1f))
                    InfoPill(label = "OAuth Brand", value = user.brandName, modifier = Modifier.weight(1f))
                }
            }
        }

        // Granted Scopes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "GRANTED OAUTH SCOPES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextMuted
                )

                ScopeItem(
                    icon = Icons.Default.Fingerprint,
                    title = "OpenID Connect (openid)",
                    desc = "Cryptographic identity assertion"
                )
                ScopeItem(
                    icon = Icons.Default.Person,
                    title = "User Profile (userinfo.profile)",
                    desc = "Display name, profile photo, and identity"
                )
                ScopeItem(
                    icon = Icons.Default.Email,
                    title = "Email Address (userinfo.email)",
                    desc = "Account verification & Firestore project sync"
                )
            }
        }

        // Cloud Sync Action
        Button(
            onClick = onSyncProjects,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sync_cloud_projects_button")
        ) {
            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Active Project to Cloud", fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Sign Out Button
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF4444)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DEF4444)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_oauth_sign_out_button")
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentRose)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold, color = AccentRose)
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x14FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScopeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x1A10B981)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = desc, fontSize = 10.sp, color = TextMuted)
        }
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AuthenticatingView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = PrimaryLight,
            modifier = Modifier.size(44.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Negotiating OAuth 2.0 tokens with matrix-decoded",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun UnauthenticatedView(
    errorMessage: String?,
    onSignIn: () -> Unit,
    onQuickConnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Branding Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x264285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sign in to DevDirector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Connected Google Cloud Project: matrix-decoded",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sign in with your Google Workspace account to securely sync promo video projects, render assets, and back up Remotion scene configurations across devices.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // Requested Permissions Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F111A))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "REQUESTED OAUTH 2.0 PERMISSIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextMuted
                )

                ScopeItem(
                    icon = Icons.Default.Fingerprint,
                    title = "OpenID (openid)",
                    desc = "Cryptographic identity assertion"
                )
                ScopeItem(
                    icon = Icons.Default.Person,
                    title = "Profile (userinfo.profile)",
                    desc = "Your name and profile avatar"
                )
                ScopeItem(
                    icon = Icons.Default.Email,
                    title = "Email (userinfo.email)",
                    desc = "rivera.carmeloiii@gmail.com"
                )
            }
        }

        // Error message if any
        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x26EF4444))
                    .border(1.dp, Color(0x66EF4444), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AccentRose, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage, fontSize = 11.sp, color = Color(0xFFFCA5A5))
                }
            }
        }

        // One-Tap Confirmed Identity Button (rivera.carmeloiii@gmail.com)
        Button(
            onClick = onQuickConnect,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("one_tap_google_oauth_button")
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4)),
                contentAlignment = Alignment.Center
            ) {
                Text("G", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Continue as Carmelo Rivera", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("rivera.carmeloiii@gmail.com", fontSize = 10.sp, color = TextMuted)
            }
        }

        // Standard Credential Manager Sign In Button
        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("google_credential_manager_button")
        ) {
            Text("Sign in with Google Account", fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        }
    }
}
