package com.example.ui.screens

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromoProject
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentEmeraldLight
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryDark
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSubtle
import com.example.ui.theme.dotMatrix
import com.example.ui.theme.immersiveGradientBackground
import com.example.ui.viewmodel.PromoViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: PromoViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToExport: () -> Unit,
    onOpenDirectorChat: () -> Unit,
    onOpenGrokOAuth: () -> Unit = {},
    onOpenGoogleOAuth: () -> Unit = {},
    onOpenAppearance: () -> Unit = {}
) {
    val projects by viewModel.projects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val currentThemeMode by viewModel.currentThemeMode.collectAsState()
    val xaiAuthState by viewModel.xaiAuthState.collectAsState()
    val isGrokConnected = xaiAuthState is com.example.service.XAiAuthState.Connected
    val googleAuthState by viewModel.googleAuthState.collectAsState()
    val isGoogleConnected = googleAuthState is com.example.service.GoogleAuthState.Authenticated
    val googleUser = (googleAuthState as? com.example.service.GoogleAuthState.Authenticated)?.user

    // Pulsing indicator animation for "Claude Code Active"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = Modifier.fillMaxSize().immersiveGradientBackground(themeMode = currentThemeMode)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Violet glow logo container (w-11 h-11 rounded-2xl bg-violet-600 glow-pulse)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PrimaryIndigo)
                                    .drawBehind {
                                        // Subtle brand glow ring
                                        drawCircle(
                                            color = Color(0x408B5CF6),
                                            radius = size.width * 0.7f
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "DevDirector",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "DevDirector",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.25).sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "CLAUDE CODE ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmeraldLight,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Google OAuth Sign-In / Account Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isGoogleConnected) Color(0x264285F4) else GlassBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isGoogleConnected) Color(0xFF4285F4) else GlassBorder
                            ),
                            modifier = Modifier
                                .clickable { onOpenGoogleOAuth() }
                                .testTag("home_google_oauth_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isGoogleConnected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4285F4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (googleUser?.displayName ?: "C").take(1).uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = googleUser?.displayName?.split(" ")?.firstOrNull() ?: "Google",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Sign in",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Sign In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // SuperGrok OAuth Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isGrokConnected) Color(0x2610B981) else GlassBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isGrokConnected) AccentEmerald else GlassBorder
                            ),
                            modifier = Modifier
                                .clickable { onOpenGrokOAuth() }
                                .testTag("home_grok_oauth_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "SuperGrok",
                                    tint = if (isGrokConnected) AccentEmerald else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isGrokConnected) "SuperGrok" else "Connect Grok",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGrokConnected) AccentEmerald else TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Appearance / Workspace Theme Selector Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlassBackground)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable { onOpenAppearance() }
                                .testTag("home_appearance_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Workspace Appearance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Terminal / AI Director Action Button (w-11 h-11 rounded-full bg-white/5 border border-white/10)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlassBackground)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable { onOpenDirectorChat() }
                                .testTag("ai_director_chat_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "AI Director Terminal",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                // Immersive Footer & Navigation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC050505))
                        .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    // Generate / Primary Action Button
                    Button(
                        onClick = onNavigateToScanner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("new_promo_project_fab"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Final MP4s",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal Immersive Bottom Nav Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Home",
                                tint = PrimaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToScanner) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Projects",
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToStudio) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Studio",
                                tint = TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = onOpenGoogleOAuth) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile & OAuth Account",
                                tint = if (isGoogleConnected) Color(0xFF4285F4) else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    // Brand Discovery / Source Scanning Section
                    SourceScanningCard(
                        currentProject = currentProject,
                        onScanClick = onNavigateToScanner
                    )
                }

                item {
                    // Settings Grid: Voice Actor & Narrative Cards
                    SettingsGrid(
                        currentProject = currentProject,
                        onOpenSettings = onNavigateToStudio
                    )
                }

                item {
                    // Workspace Theme Appearance Settings
                    WorkspaceThemeCard(
                        currentThemeMode = currentThemeMode,
                        onOpenAppearance = onOpenAppearance
                    )
                }

                item {
                    // Real-time Scene Builder / Render Preview
                    RenderPreviewCard(onOpenStudio = onNavigateToStudio)
                }

                item {
                    // Preflight Environment Status Card
                    PreflightStatusCard(
                        isGrokConnected = isGrokConnected,
                        onOpenGrokOAuth = onOpenGrokOAuth,
                        isGoogleConnected = isGoogleConnected,
                        onOpenGoogleOAuth = onOpenGoogleOAuth
                    )
                }

                item {
                    // Promo Projects Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Promo Projects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "${projects.size} ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(projects, key = { it.id }) { project ->
                    ProjectCardItem(
                        project = project,
                        isSelected = currentProject?.id == project.id,
                        onSelect = {
                            viewModel.loadProject(project)
                            onNavigateToStudio()
                        },
                        onDelete = {
                            viewModel.deleteProject(project)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * Brand Discovery / Source Scanning Section from the Immersive UI design
 */
@Composable
private fun SourceScanningCard(
    currentProject: PromoProject?,
    onScanClick: () -> Unit
) {
    val brandName = currentProject?.brandName?.ifEmpty { "fastsolve.app" } ?: "fastsolve.app"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row: SOURCE SCANNING & Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOURCE SCANNING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryLight)
                    )
                    Text(
                        text = "82% Scan Complete",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content Row: Rocket Icon & Brand Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rocket Launch gradient badge (w-16 h-16 rounded-2xl bg-gradient-to-br from-orange-500 to-red-600)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentOrange, AccentRed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = brandName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Found branding in tailwind.config.js and logo.svg",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sleek progress bar (h-1.5 w-full bg-white/10 rounded-full)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryLight)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Trigger Button
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F7C3AED)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_repo_hero_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = SecondaryLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Repo & Discover Brand",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SecondaryLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Settings Grid: Voice Actor and Narrative Cards from the Immersive UI design
 */
@Composable
private fun SettingsGrid(
    currentProject: PromoProject?,
    onOpenSettings: () -> Unit
) {
    val voiceName = currentProject?.voiceActor ?: "Adam"
    val narrative = currentProject?.narrativeTemplate?.replace("_", " ") ?: "Rage Hook"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Voice Actor Card (rounded-[28px] bg-white/5 border border-white/10 p-4)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                .clickable { onOpenSettings() }
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VOICE ACTOR",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x338B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = SecondaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = voiceName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Narrative Card (rounded-[28px] bg-white/5 border border-white/10 p-4)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                .clickable { onOpenSettings() }
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NARRATIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F97316)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = narrative,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Real-time Scene Builder / Render Preview from the Immersive UI design
 */
@Composable
private fun RenderPreviewCard(onOpenStudio: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .clickable { onOpenStudio() }
            .padding(18.dp)
    ) {
        Column {
            // Header Row: RENDER PREVIEW & Scene status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RENDER PREVIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Scene 4 of 12",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Format Canvas: Portrait + Landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Portrait View (9:16)
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1E))
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                ) {
                    // Vignette gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x99000000),
                                        Color(0xE6000000)
                                    )
                                )
                            )
                    )

                    // Center ghost play circle
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0x33FFFFFF),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Bottom subtitle placeholders
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x4DFFFFFF))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x33FFFFFF))
                        )
                    }
                }

                // Landscape View (16:9) with Dot Matrix Texture
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E1E22), Color(0xFF141417))
                            )
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .dotMatrix(dotColor = Color(0x2E4F46E5), spacingPx = 20f, dotRadiusPx = 1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "SCENE 4 / METALLIC SWOOSH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = SecondaryLight,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"Tired of setting up\nffmpeg manually?\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                    }

                    // Bottom Progress Strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(4.dp)
                                .background(PrimaryLight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreflightStatusCard(
    isGrokConnected: Boolean,
    onOpenGrokOAuth: () -> Unit,
    isGoogleConnected: Boolean,
    onOpenGoogleOAuth: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PREFLIGHT ENVIRONMENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TextMuted
                )
                Text(
                    text = if (isGoogleConnected && isGrokConnected) "OAUTH ACTIVE" else if (isGoogleConnected) "GOOGLE AUTH OK" else "ALL SYSTEMS NOMINAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.clickable { onOpenGoogleOAuth() }) {
                    PreflightCheckItem("Google OAuth", if (isGoogleConnected) "matrix-decoded" else "Sign In", isGoogleConnected)
                }
                Box(modifier = Modifier.clickable { onOpenGrokOAuth() }) {
                    PreflightCheckItem("SuperGrok", if (isGrokConnected) "OAuth OK" else "Connect", isGrokConnected)
                }
                PreflightCheckItem("Claude Code", "v1.2", true)
                PreflightCheckItem("Voiceover", "ElevenLabs", true)
            }
        }
    }
}

@Composable
private fun PreflightCheckItem(name: String, version: String, isOk: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isOk) AccentEmerald else Color(0xFFEF4444),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(text = name, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(text = version, fontSize = 9.sp, color = TextMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCardItem(
    project: PromoProject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(project.primaryColorHex))
    } catch (_: Exception) { PrimaryIndigo }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) Color(0x1A8B5CF6) else GlassBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PrimaryLight else GlassBorder,
                shape = RoundedCornerShape(28.dp)
            )
            .clickable { onSelect() }
            .testTag("project_card_${project.id}")
            .padding(18.dp)
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = project.brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = project.repoUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = project.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Badges in Immersive Glass Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = project.narrativeTemplate.replace("_", " "),
                        fontSize = 11.sp,
                        color = AccentAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = "${project.targetDurationSeconds}s Duration",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = "Voice: ${project.voiceActor}",
                        fontSize = 11.sp,
                        color = AccentEmerald,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = "Dual 16:9 + 9:16",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_studio_button_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Video Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Workspace Theme Card: Quick access to DevDirector's 5 theme modes from home
 */
@Composable
private fun WorkspaceThemeCard(
    currentThemeMode: AppThemeMode,
    onOpenAppearance: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clickable { onOpenAppearance() }
            .padding(16.dp)
            .testTag("workspace_theme_dashboard_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x266366F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Appearance",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "WORKSPACE THEME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(currentThemeMode.previewPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentThemeMode.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentThemeMode.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Color preview dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(currentThemeMode.previewPrimary))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(currentThemeMode.previewSecondary))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(currentThemeMode.previewBackground).border(0.5.dp, Color.Gray.copy(alpha = 0.5f), CircleShape))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Customize Theme",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

