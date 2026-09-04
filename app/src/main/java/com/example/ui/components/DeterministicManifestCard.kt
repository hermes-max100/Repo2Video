package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.ClaimEvidence
import com.example.data.model.DeterministicScanManifest
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeterministicManifestCard(
    manifest: DeterministicScanManifest,
    onUpdateManifest: (DeterministicScanManifest) -> Unit,
    onDeleteRawSource: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(manifest) { mutableStateOf(manifest.appName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deterministic_manifest_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x336366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Deterministic Scan Manifest", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Curated structured source of truth", style = MaterialTheme.typography.labelSmall, color = AccentEmerald)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Manifest", tint = PrimaryLight, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Inferred / Editable App Name
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Product Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onUpdateManifest(manifest.copy(appName = editedName))
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Manifest Name")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TARGET APPLICATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                        Text(manifest.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0x3310B981)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zero Raw Retention", fontSize = 10.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Technologies
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("VERIFIED TECHNOLOGIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    manifest.technologies.forEach { tech ->
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0x228B5CF6)) {
                            Text(text = tech, fontSize = 11.sp, color = SecondaryLight, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            // Key Features
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("KEY FEATURES & VALUE PROPOSITIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                manifest.keyFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• ", color = AccentCyan, fontWeight = FontWeight.Bold)
                        Text(feature, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            // Claim Evidence Linking Table
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLAIM EVIDENCE AUDIT (SOURCE PROOF)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentAmber, letterSpacing = 1.sp)
                }

                manifest.claimsWithEvidence.forEach { claim ->
                    ClaimEvidenceRow(claim = claim)
                }
            }

            // Delete Source Data Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1A000000))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Local Source Data", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Purge cached git metadata & temp analysis", fontSize = 10.sp, color = TextMuted)
                }
                Button(
                    onClick = onDeleteRawSource,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("delete_source_data_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = AccentRose, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Purge Data", color = AccentRose, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ClaimEvidenceRow(claim: ClaimEvidence) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x11FFFFFF))
            .border(1.dp, if (claim.isVerified) Color(0x3310B981) else Color(0x33F59E0B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = claim.claimText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (claim.isVerified) {
                    Surface(shape = CircleShape, color = Color(0x3310B981)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Verified", fontSize = 9.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(shape = CircleShape, color = Color(0x33F59E0B)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Unverified", fontSize = 9.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${claim.sourceFile}:${claim.lineReference}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("•", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = claim.evidenceSnippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
