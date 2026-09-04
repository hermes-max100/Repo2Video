package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.XAiAuthState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PromoViewModel

@Composable
fun GrokOAuthDialog(
    viewModel: PromoViewModel,
    onDismiss: () -> Unit
) {
    val authState by viewModel.xaiAuthState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showManualTokenInput by remember { mutableStateOf(false) }
    var manualTokenText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("grok_oauth_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF50D0E15)),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with xAI Grok logo and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                    )
                                )
                                .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "xAI Grok",
                                tint = AccentEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "xAI Grok OAuth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2610B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "SuperGrok",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    )
                                }
                            }
                            Text(
                                text = "auth.x.ai • Device Authorization Grant",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_grok_oauth_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Body content based on OAuth state
                when (val state = authState) {
                    is XAiAuthState.Disconnected -> {
                        DisconnectedStateView(
                            onStartOAuth = { viewModel.startXAiOAuth() },
                            onToggleManual = { showManualTokenInput = !showManualTokenInput },
                            showManualInput = showManualTokenInput,
                            manualTokenText = manualTokenText,
                            onManualTokenChange = { manualTokenText = it },
                            onSubmitManualToken = {
                                viewModel.connectXAiWithToken(manualTokenText)
                            }
                        )
                    }

                    is XAiAuthState.RequestingCode -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = AccentEmerald,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    is XAiAuthState.AwaitingApproval -> {
                        AwaitingApprovalView(
                            state = state,
                            pulseAlpha = pulseAlpha,
                            onCopyCode = {
                                clipboardManager.setText(AnnotatedString(state.userCode))
                                Toast.makeText(context, "Code copied: ${state.userCode}", Toast.LENGTH_SHORT).show()
                            },
                            onOpenVerification = {
                                clipboardManager.setText(AnnotatedString(state.userCode))
                                val url = state.verificationUriComplete ?: state.verificationUri
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onSimulateApproval = {
                                viewModel.simulateXAiOAuthApproval()
                            },
                            onCancel = {
                                viewModel.disconnectXAi()
                            }
                        )
                    }

                    is XAiAuthState.Connected -> {
                        ConnectedStateView(
                            state = state,
                            onDisconnect = { viewModel.disconnectXAi() },
                            onDone = onDismiss
                        )
                    }

                    is XAiAuthState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AF43F5E))
                                .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Authentication Error",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = Color(0xFFFDA4AF),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.startXAiOAuth() },
                                    modifier = Modifier.weight(1f).testTag("retry_oauth_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry OAuth", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.simulateXAiOAuthApproval() },
                                    modifier = Modifier.weight(1f).testTag("bypass_simulate_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Test Offline", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedStateView(
    onStartOAuth: () -> Unit,
    onToggleManual: () -> Unit,
    showManualInput: Boolean,
    manualTokenText: String,
    onManualTokenChange: (String) -> Unit,
    onSubmitManualToken: () -> Unit
) {
    Column {
        // SuperGrok feature explanation card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0F10B981))
                .border(1.dp, AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "SuperGrok Subscription Benefits",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Authenticate via OAuth using your SuperGrok account to generate high-energy promo scripts, emotional voiceover hooks, and Director intelligence with xAI Grok 2 models.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main OAuth button
        Button(
            onClick = onStartOAuth,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_grok_oauth_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Authenticate with SuperGrok (OAuth)",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manual token alternative toggle
        TextButton(
            onClick = onToggleManual,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showManualInput) "Hide manual token input" else "Or connect with API Key / Token manually",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        AnimatedVisibility(visible = showManualInput) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = manualTokenText,
                    onValueChange = onManualTokenChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_xai_token_input"),
                    placeholder = { Text("Paste xAI Key or SuperGrok Token", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSubmitManualToken,
                    modifier = Modifier.fillMaxWidth().testTag("submit_manual_token_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = manualTokenText.isNotBlank()
                ) {
                    Text("Save & Connect Token", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun AwaitingApprovalView(
    state: XAiAuthState.AwaitingApproval,
    pulseAlpha: Float,
    onCopyCode: () -> Unit,
    onOpenVerification: () -> Unit,
    onSimulateApproval: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step 1: Code display box
        Text(
            text = "ENTER THIS CODE ON XAI",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AccentEmerald,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, AccentEmerald.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { onCopyCode() }
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .testTag("grok_user_code_box"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = state.userCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = AccentEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Open xAI verification URL button
        Button(
            onClick = onOpenVerification,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_xai_verification_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Copy Code & Open auth.x.ai/activate",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Live polling radar animation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald.copy(alpha = pulseAlpha))
            )
            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            val timeStr = String.format("%02d:%02d", minutes, seconds)
            Text(
                text = "Listening for approval on auth.x.ai ($timeStr)",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick simulation & cancel actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSimulateApproval,
                modifier = Modifier.weight(1f).testTag("simulate_approval_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A10B981)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simulate Approve", fontSize = 11.sp, color = AccentEmerald)
            }

            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("cancel_oauth_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ConnectedStateView(
    state: XAiAuthState.Connected,
    onDisconnect: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0x2610B981))
                .border(2.dp, AccentEmerald, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SuperGrok Connected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = state.accountName,
            fontSize = 12.sp,
            color = AccentEmerald
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection detail pills
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Model Access", fontSize = 11.sp, color = TextMuted)
                    Text("grok-2-latest", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auth Type", fontSize = 11.sp, color = TextMuted)
                    Text(state.accountType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Session Token", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "xai-••••" + state.accessToken.takeLast(6),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f).testTag("disconnect_xai_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AF43F5E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Disconnect", color = AccentRed, fontSize = 12.sp)
            }

            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f).testTag("done_oauth_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ready to Create", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
