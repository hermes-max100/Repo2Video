package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IngestionConsentPolicy
import com.example.data.model.SanitizedPayload
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun IngestionConsentDialog(
    initialPolicy: IngestionConsentPolicy,
    sanitizedPayload: SanitizedPayload?,
    onDismiss: () -> Unit,
    onPreviewSanitization: (IngestionConsentPolicy) -> Unit,
    onConfirmIngestion: (IngestionConsentPolicy) -> Unit
) {
    var policy by remember { mutableStateOf(initialPolicy) }
    var showPreviewModal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ingestion_consent_dialog"),
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3310B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Repository Ingestion Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Zero-leakage privacy & sensitive file filtering",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentEmerald
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Policy explanation card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PRIVACY & SECURITY COMMITMENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 1.sp
                        )
                        BulletPoint(text = "What is read: Only README, package.json/build manifests, and license files.")
                        BulletPoint(text = "What is sent to AI: Compact sanitized manifest only. Deny-by-default secret filtering strips keys, tokens, and private paths.")
                        BulletPoint(text = "Firebase cloud sync: Controlled by your backup preferences. No raw source is ever uploaded.")
                        BulletPoint(text = "Immediate deletion: 1-click 'Purge Source Data' control available at any time.")
                    }
                }

                Text(
                    text = "DENY-BY-DEFAULT EXCLUSIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )

                ExclusionToggleRow(
                    label = "Exclude .env & credentials",
                    description = "Blocks .env, .env.*, credentials.json, google-services.json",
                    checked = policy.excludeDotEnv,
                    onCheckedChange = { policy = policy.copy(excludeDotEnv = it) }
                )

                ExclusionToggleRow(
                    label = "Exclude Keystores & Private Keys",
                    description = "Blocks *.keystore, *.jks, *.pem, id_rsa, and SSL certificates",
                    checked = policy.excludeKeystores,
                    onCheckedChange = { policy = policy.copy(excludeKeystores = it) }
                )

                ExclusionToggleRow(
                    label = "Exclude Lockfiles",
                    description = "Bypasses package-lock.json, yarn.lock, Cargo.lock (saves bandwidth)",
                    checked = policy.excludeLockfiles,
                    onCheckedChange = { policy = policy.copy(excludeLockfiles = it) }
                )

                ExclusionToggleRow(
                    label = "Exclude Build Artifacts & Dist",
                    description = "Ignores build/, dist/, target/, out/ bundles",
                    checked = policy.excludeBuildArtifacts,
                    onCheckedChange = { policy = policy.copy(excludeBuildArtifacts = it) }
                )

                ExclusionToggleRow(
                    label = "Exclude Vendor Packages",
                    description = "Bypasses node_modules/, vendor/, Carthage/ directories",
                    checked = policy.excludeVendorDirs,
                    onCheckedChange = { policy = policy.copy(excludeVendorDirs = it) }
                )

                // Authority confirmation
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33F59E0B)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66F59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = policy.contentAuthorityConfirmed,
                            onCheckedChange = { policy = policy.copy(contentAuthorityConfirmed = it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentAmber,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("authority_consent_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I confirm I have authority to generate promotional marketing for this software and will not create deceptive claims or impersonate authors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Sanitization Preview button
                OutlinedButton(
                    onClick = {
                        onPreviewSanitization(policy)
                        showPreviewModal = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("preview_sanitized_payload_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Sanitized Payload")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmIngestion(policy) },
                enabled = policy.contentAuthorityConfirmed,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_ingestion_button")
            ) {
                Text("Approve & Ingest", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    // Preview Dialog
    if (showPreviewModal && sanitizedPayload != null) {
        AlertDialog(
            onDismissRequest = { showPreviewModal = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sanitized Payload Preview", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge(label = "Secrets Masked", value = "${sanitizedPayload.blockedSecretCount}", color = AccentEmerald)
                        StatBadge(label = "Files Excluded", value = "${sanitizedPayload.excludedFileCount}", color = AccentAmber)
                        StatBadge(label = "Budget Chars", value = "${sanitizedPayload.sanitizedCharacterCount}", color = AccentCyan)
                    }

                    Text(
                        text = "SANITIZED PAYLOAD TO TRANSMIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF020617))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = sanitizedPayload.sanitizedContentPreview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreviewModal = false }) {
                    Text("Close Preview")
                }
            }
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("• ", color = AccentCyan, fontWeight = FontWeight.Bold)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
private fun ExclusionToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x11FFFFFF))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = description, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentEmerald,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Text(text = label, fontSize = 9.sp, color = TextSecondary)
        }
    }
}
