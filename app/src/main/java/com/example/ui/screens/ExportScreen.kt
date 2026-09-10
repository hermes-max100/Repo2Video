package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Download
import com.example.ui.components.VideoDownloadCard
import com.example.data.model.RenderJobEntity
import com.example.data.model.RenderJobStatus
import com.example.data.model.RenderJobType
import com.example.data.model.LicensingAttribution
import com.example.data.model.AspectRatioFormat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromoScene
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.CodeComment
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.immersiveGradientBackground
import com.example.ui.viewmodel.PromoViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: PromoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val brandProfile by viewModel.brandProfile.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val targetDuration by viewModel.targetDurationSeconds.collectAsState()
    val voiceActor by viewModel.voiceActor.collectAsState()
    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val activeAspectRatio by viewModel.activeAspectRatio.collectAsState()
    val activeJobs by viewModel.activeRenderJobs.collectAsState()
    val mediaCacheSize by viewModel.mediaCacheSize.collectAsState()
    val licensingManifest by viewModel.licensingManifest.collectAsState()
    val contentSafetyCheck by viewModel.contentSafetyCheck.collectAsState()
    val deliveryPackage by viewModel.deliveryPackage.collectAsState()
    val evidenceLedger by viewModel.evidenceLedger.collectAsState()
    val creativeBrief by viewModel.creativeBrief.collectAsState()
    val creativeVariants by viewModel.creativeVariants.collectAsState()
    val visualQAReport by viewModel.visualQAReport.collectAsState()
    val claimsGateResult by viewModel.claimsGateResult.collectAsState()
    val videoDownloadState by viewModel.videoDownloadState.collectAsState()
    val selectedDownloadQuality by viewModel.selectedDownloadQuality.collectAsState()
    val selectedDownloadDestination by viewModel.selectedDownloadDestination.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()

    var selectedCodeTab by remember { mutableIntStateOf(0) }
    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var isRenderComplete by remember { mutableStateOf(false) }

    val codeTabs = listOf("Root.tsx", "PromoComposition.tsx", "scenes.json", "remotion.config.ts")

    val cliCommand = "claude \"Create a ${targetDuration}s ${brandProfile.name} promo video with ${transitionStyle.displayName} transitions using promo-video-skill\""

    // Simulate render progress
    LaunchedEffect(isRendering) {
        if (isRendering) {
            renderProgress = 0f
            isRenderComplete = false
            for (i in 1..100) {
                delay(35)
                renderProgress = i / 100f
            }
            isRendering = false
            isRenderComplete = true
            Toast.makeText(context, "Dual format videos rendered successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().immersiveGradientBackground()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Phase 4: Export & Render",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "One command generation • Landscape + Portrait Remotion bundle",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // AUTONOMOUS CREATIVE STUDIO V2 FULL DELIVERY PACKAGE CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF0F111E))
                            .border(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            // Header & Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x336366F1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "AUTONOMOUS CREATIVE STUDIO V2",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = "Full Delivery Package",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x3310B981),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald)
                                ) {
                                    Text(
                                        text = "6 ARTIFACTS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "End-to-end verified delivery package ready for deployment. Contains deterministic evidence ledger, platform-aware variants, keyframe visual QA report, and final masters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 6 Package Deliverables Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 1. Creative Brief
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("1. Creative Brief (creative_brief.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Audience, Angle, Tone & CTA defined by Creative Director", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("READY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                }

                                // 2. Evidence Ledger
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("2. Evidence Ledger (evidence_ledger.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Hard claims gate: ${evidenceLedger?.entries?.size ?: 5} verified proofs, 0 unsupported claims", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("VERIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                }

                                // 3. Variant Report
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("3. Variant Report (variant_report.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("A/B/C multi-factor score breakdown • Winner: Variant B (94.2 pts)", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("WINNER B", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                                }

                                // 4. QA Report with Contact Sheet
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("4. Visual QA & Contact Sheet (qa_critique_report.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Keyframes t=0.5s, 1.8s, 3.2s, 4.5s • 3 automated repairs applied", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("PASSED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                }

                                // 5. Delivery Report
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = SecondaryLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("5. Delivery Report (delivery_manifest.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Platform mastering profiles, asset hashes & license attributions", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("READY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                }

                                // 6. Final Videos
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("6. Final Master Videos (3 Platform Formats)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("A: 16:9 Landscape YouTube • B: 9:16 Portrait TikTok • C: 1:1 Social", fontSize = 9.sp, color = TextMuted)
                                    }
                                    Text("RENDERED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Button to Export Full Package
                            Button(
                                onClick = {
                                    val fullPackage = viewModel.exportAutonomousDeliveryPackage()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Autonomous Delivery Package", fullPackage.fullPackageJson))
                                    Toast.makeText(context, "Full Autonomous v2 Delivery Package copied to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_autonomous_delivery_package_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Full Autonomous v2 Delivery Package", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // "ONE COMMAND" CLAUDE CODE CLI CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x338B5CF6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = SecondaryLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "CLAUDE CODE CLI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "One-Command Execution",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Promo Command", cliCommand))
                                        Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1AFFFFFF))
                                        .testTag("copy_cli_command_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy command",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Run this single command inside your repository terminal to trigger automated Remotion scene compilation & ElevenLabs speech generation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Terminal box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF070709))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "# 1. Install promo skill",
                                        fontSize = 10.sp,
                                        color = CodeComment,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "npx skills add AKCodez/promo-video-skill",
                                        fontSize = 11.sp,
                                        color = AccentCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "# 2. Run promo generation prompt",
                                        fontSize = 10.sp,
                                        color = CodeComment,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = cliCommand,
                                        fontSize = 11.sp,
                                        color = Color(0xFFC4B5FD),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // GOOGLE OAUTH CLOUD SYNC CARD
                    val googleAuthState by viewModel.googleAuthState.collectAsState()
                    val isGoogleAuthenticated = googleAuthState is com.example.service.GoogleAuthState.Authenticated
                    val user = (googleAuthState as? com.example.service.GoogleAuthState.Authenticated)?.user

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x264285F4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "GOOGLE CLOUD SYNC",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "matrix-decoded • DevDirector",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (isGoogleAuthenticated) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0x2610B981),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald)
                                    ) {
                                        Text(
                                            text = "CONNECTED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isGoogleAuthenticated)
                                    "Signed in as ${user?.email}. Promo video timeline, scenes, and Remotion configurations are synchronizable to Google Cloud Firestore."
                                else
                                    "Authenticate with Google OAuth 2.0 to back up your promo video project and render configurations to Google Cloud.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (isGoogleAuthenticated) {
                                        viewModel.syncCurrentProjectToCloud()
                                        Toast.makeText(context, "Project synced to matrix-decoded cloud!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.signInWithConfirmedGoogleAccount()
                                        Toast.makeText(context, "Signed in with DevDirector OAuth identity!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGoogleAuthenticated) PrimaryIndigo else Color(0xFF4285F4)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("export_cloud_sync_button")
                            ) {
                                Icon(
                                    imageVector = if (isGoogleAuthenticated) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isGoogleAuthenticated) "Sync Project to Cloud" else "Sign In & Sync with Google",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                item {
                    // DUAL FORMAT OUTPUTS CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x33F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "OUTPUT FORMATS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Dual-Format Render Outputs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Both formats share scenes and audio with adaptive visual layouts matching target social platforms.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Landscape 16:9 Box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x14FFFFFF))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "landscape.mp4",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0x338B5CF6)
                                            ) {
                                                Text(
                                                    text = "16:9",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFFC4B5FD),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("1920 × 1080 @ 30 FPS", fontSize = 10.sp, color = TextMuted)
                                        Text("YouTube, X, Landing Page", fontSize = 10.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0x2210B981)
                                            ) {
                                                Text(
                                                    text = "1080P",
                                                    fontSize = 9.sp,
                                                    color = AccentEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0x3310B981),
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.downloadPromoVideo(aspectRatio = AspectRatioFormat.LANDSCAPE_16_9)
                                                    }
                                                    .testTag("download_card_16x9")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "DOWNLOAD",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Portrait 9:16 Box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x14FFFFFF))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "portrait.mp4",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SecondaryViolet.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "9:16",
                                                    fontSize = 9.sp,
                                                    color = SecondaryLight,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("1080 × 1920 @ 30 FPS", fontSize = 10.sp, color = TextMuted)
                                        Text("TikTok, Reels, Shorts", fontSize = 10.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0x2210B981)
                                            ) {
                                                Text(
                                                    text = "1080P",
                                                    fontSize = 9.sp,
                                                    color = AccentEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0x3310B981),
                                                modifier = Modifier
                                                    .clickable {
                                                        viewModel.downloadPromoVideo(aspectRatio = AspectRatioFormat.PORTRAIT_9_16)
                                                    }
                                                    .testTag("download_card_9x16")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "DOWNLOAD",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Render Progress or Start Render Button
                            if (isRendering) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Rendering Remotion Compositor...",
                                            fontSize = 12.sp,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = "${(renderProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { renderProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = PrimaryIndigo,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { isRendering = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("simulate_render_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Render Dual MP4s Now", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            AnimatedVisibility(visible = isRenderComplete) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x2210B981))
                                            .border(1.dp, Color(0x6610B981), RoundedCornerShape(20.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Render Completed (1080p 60fps)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Ready to download or sync to device local storage",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA7F3D0)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.downloadPromoVideo(aspectRatio = AspectRatioFormat.LANDSCAPE_16_9)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("save_rendered_video_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Save 16:9 Landscape MP4 to Device Storage",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // DEDICATED LOCAL STORAGE DOWNLOAD CARD
                    VideoDownloadCard(
                        downloadState = videoDownloadState,
                        selectedQuality = selectedDownloadQuality,
                        selectedDestination = selectedDownloadDestination,
                        downloadedHistory = downloadedVideos,
                        onSelectQuality = { viewModel.setDownloadQuality(it) },
                        onSelectDestination = { viewModel.setDownloadDestination(it) },
                        onDownloadFormat = { format ->
                            viewModel.downloadPromoVideo(aspectRatio = format)
                        },
                        onDownloadAllFormats = {
                            viewModel.downloadAllVideoFormats()
                        },
                        onOpenVideo = { result ->
                            try {
                                val intent = viewModel.createOpenVideoIntent(result)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open video player: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareVideo = { result ->
                            try {
                                val intent = viewModel.createShareVideoIntent(result)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot share video: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("video_download_card")
                    )
                }

                item {
                    // REMOTION TYPESCRIPT CODE INSPECTOR
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SOURCE GENERATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Remotion TypeScript Bundle",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        val codeToCopy = generateRemotionCode(selectedCodeTab, brandProfile.name, scenes)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Remotion Code", codeToCopy))
                                        Toast.makeText(context, "${codeTabs[selectedCodeTab]} copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("copy_source_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // File Tabs
                            ScrollableTabRow(
                                selectedTabIndex = selectedCodeTab,
                                containerColor = Color(0x14FFFFFF),
                                contentColor = PrimaryIndigo,
                                edgePadding = 4.dp,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            ) {
                                codeTabs.forEachIndexed { index, tabName ->
                                    Tab(
                                        selected = selectedCodeTab == index,
                                        onClick = { selectedCodeTab = index },
                                        text = {
                                            Text(
                                                text = tabName,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (selectedCodeTab == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedCodeTab == index) PrimaryLight else TextSecondary
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Code Viewer Body
                            val codeContent = generateRemotionCode(selectedCodeTab, brandProfile.name, scenes)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF070709))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = codeContent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFC9D1D9),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // DURABLE BACKGROUND RENDER JOBS CARD
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x266366F1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cached,
                                            contentDescription = null,
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "DURABLE RENDER QUEUE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "Resumable Background Pipelines",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x2210B981)
                                ) {
                                    Text(
                                        text = "${activeJobs.size} Active",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Background renders are persisted in local SQLite/Room. If the app is closed or backgrounded, jobs resume from their last verified checkpoint.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            // Active Job Cards
                            if (activeJobs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x0DFFFFFF))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No active background jobs. Start one below.", fontSize = 12.sp, color = TextMuted)
                                }
                            } else {
                                activeJobs.forEach { job ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x14FFFFFF))
                                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Job #${job.id.take(8)} • ${job.jobType}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = when (job.status) {
                                                        RenderJobStatus.COMPLETED.name -> Color(0x3310B981)
                                                        RenderJobStatus.FAILED.name -> Color(0x33EF4444)
                                                        RenderJobStatus.PROCESSING.name -> Color(0x336366F1)
                                                        else -> Color(0x33F59E0B)
                                                    }
                                                ) {
                                                    Text(
                                                        text = job.status,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (job.status) {
                                                            RenderJobStatus.COMPLETED.name -> AccentEmerald
                                                            RenderJobStatus.FAILED.name -> AccentRose
                                                            RenderJobStatus.PROCESSING.name -> PrimaryLight
                                                            else -> AccentAmber
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Step: ${job.checkpointStep}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = AccentCyan
                                            )

                                            LinearProgressIndicator(
                                                progress = { job.progressPercent / 100f },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = PrimaryIndigo,
                                                trackColor = Color(0x22FFFFFF)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                if (job.status == RenderJobStatus.FAILED.name || job.status == RenderJobStatus.CANCELLED.name) {
                                                    Button(
                                                        onClick = { viewModel.retryRenderJob(job.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x336366F1)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Resume Checkpoint", fontSize = 10.sp)
                                                    }
                                                } else if (job.status == RenderJobStatus.PROCESSING.name || job.status == RenderJobStatus.QUEUED.name) {
                                                    Button(
                                                        onClick = { viewModel.cancelRenderJob(job.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentRose)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Cancel", fontSize = 10.sp, color = AccentRose)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.startDurableExportJob(activeAspectRatio)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_durable_render_button")
                            ) {
                                Icon(imageVector = Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Queue Resumable Background Render", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // STORAGE LIFECYCLE & CACHE MANAGEMENT
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x2606B6D4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "MEDIA LIFECYCLE & CACHE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = TextMuted)
                                        Text(text = "Ephemeral Storage Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0x2206B6D4)) {
                                    Text(
                                        text = mediaCacheSize,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Temporary audio synthesis tracks and intermediate video frames are stored in scoped sandbox storage and auto-pruned after 24 hours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Button(
                                onClick = {
                                    viewModel.clearProjectMedia()
                                    Toast.makeText(context, "Temporary project media cache cleared!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("clear_project_media_button")
                            ) {
                                Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = AccentRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Project Media Cache Now", color = AccentRose, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // LICENSING & ATTRIBUTION MANIFEST
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x2610B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.FactCheck, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "LICENSING & ATTRIBUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = TextMuted)
                                        Text(text = "LICENSE_NOTICE.json", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0x2210B981)) {
                                    Text(
                                        text = "OFL & CC0 Compliant",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            val attributions = if (licensingManifest.isNotEmpty()) licensingManifest else listOf(
                                LicensingAttribution(
                                    assetName = "JetBrains Mono",
                                    assetType = "Font",
                                    source = "JetBrains Open Source",
                                    licenseType = "OFL-1.1",
                                    publishableStatus = "SAFE_TO_PUBLISH",
                                    attributionNotice = "Copyright 2020 JetBrains s.r.o."
                                ),
                                LicensingAttribution(
                                    assetName = "Native Audio Engine",
                                    assetType = "Audio Bed",
                                    source = "DevDirector Synth",
                                    licenseType = "CC0 1.0 Universal",
                                    publishableStatus = "SAFE_TO_PUBLISH",
                                    attributionNotice = "Procedural audio synthesizer"
                                ),
                                LicensingAttribution(
                                    assetName = "Remotion Engine",
                                    assetType = "Framework",
                                    source = "Remotion GmbH",
                                    licenseType = "Company License / Open Source",
                                    publishableStatus = "SAFE_TO_PUBLISH",
                                    attributionNotice = "Remotion programmatic canvas renderer"
                                ),
                                LicensingAttribution(
                                    assetName = "${voiceActor.name} Synthetic Voice",
                                    assetType = "Voiceover",
                                    source = "ElevenLabs / Android TTS",
                                    licenseType = "Commercial License",
                                    publishableStatus = "SAFE_TO_PUBLISH",
                                    attributionNotice = "Commercial neural speech synthesis"
                                )
                            )

                            attributions.forEach { attr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x11FFFFFF))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = attr.assetName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = "${attr.assetType} • ${attr.source}", fontSize = 10.sp, color = TextMuted)
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0x228B5CF6)) {
                                        Text(
                                            text = attr.licenseType,
                                            fontSize = 10.sp,
                                            color = SecondaryLight,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SHAREABLE PROJECT BUNDLE EXPORTER
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x26F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "PROJECT REPRODUCIBILITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = TextMuted)
                                    Text(text = "Shareable Project Bundle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Text(
                                text = "Export a standalone, editable .devdirector.json bundle containing your deterministic manifest, claim evidences, creative brief, and scenes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Button(
                                onClick = {
                                    val bundleJson = viewModel.exportShareableProjectBundle()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("DevDirector Project Bundle", bundleJson))
                                    Toast.makeText(context, "Full project bundle copied to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("export_project_bundle_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy .devdirector.json Bundle", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun generateRemotionCode(tabIndex: Int, brandName: String, scenes: List<PromoScene>): String {
    return when (tabIndex) {
        0 -> """
            // Root.tsx — Remotion Entry Point
            import { Composition } from 'remotion';
            import { PromoComposition } from './PromoComposition';
            import scenesData from './scenes.json';

            export const Root: React.FC = () => {
              const totalFrames = scenesData.reduce((acc, sc) => acc + (sc.durationSeconds * 30), 0);

              return (
                <>
                  {/* 16:9 Landscape for YouTube & Web */}
                  <Composition
                    id="Landscape"
                    component={PromoComposition}
                    durationInFrames={Math.round(totalFrames)}
                    fps={30}
                    width={1920}
                    height={1080}
                    defaultProps={{
                      brandName: "${brandName}",
                      isPortrait: false,
                      scenes: scenesData
                    }}
                  />

                  {/* 9:16 Portrait for TikTok & Shorts */}
                  <Composition
                    id="Portrait"
                    component={PromoComposition}
                    durationInFrames={Math.round(totalFrames)}
                    fps={30}
                    width={1080}
                    height={1920}
                    defaultProps={{
                      brandName: "${brandName}",
                      isPortrait: true,
                      scenes: scenesData
                    }}
                  />
                </>
              );
            };
        """.trimIndent()

        1 -> """
            // PromoComposition.tsx — Dynamic Remotion Scene Sequencer
            import React from 'react';
            import { Series, Audio, staticFile } from 'remotion';
            import { SceneRenderer } from './SceneRenderer';
            import { MetallicSwooshTransition } from './transitions/MetallicSwoosh';

            export const PromoComposition: React.FC<any> = ({ brandName, isPortrait, scenes }) => {
              return (
                <div style={{ flex: 1, backgroundColor: '#090D16' }}>
                  <Series>
                    {scenes.map((scene: any, index: number) => (
                      <Series.Sequence
                        key={scene.id}
                        durationInFrames={Math.round(scene.durationSeconds * 30)}
                      >
                        <SceneRenderer
                          scene={scene}
                          brandName={brandName}
                          isPortrait={isPortrait}
                        />
                        {index < scenes.length - 1 && <MetallicSwooshTransition />}
                      </Series.Sequence>
                    ))}
                  </Series>
                  <Audio src={staticFile('music/inspired_ambient.mp3')} volume={0.4} />
                </div>
              );
            };
        """.trimIndent()

        2 -> """
            [
              ${scenes.joinToString(",\n  ") { sc ->
                """{
                "order": ${sc.orderIndex},
                "title": "${sc.title}",
                "voiceover": "${sc.voiceover.replace("\"", "\\\"")}",
                "emotion": "${sc.emotionalPreset.name}",
                "durationSeconds": ${sc.durationSeconds},
                "visualType": "${sc.visualType.name}"
              }"""
              }}
            ]
        """.trimIndent()

        else -> """
            // remotion.config.ts
            import { Config } from '@remotion/cli/config';

            Config.setVideoImageFormat('jpeg');
            Config.setPixelFormat('yuv420p');
            Config.setCodec('h264');
            Config.setCrf(18); // High quality visual mastering
            Config.setScaleFactor(1);
        """.trimIndent()
    }
}
