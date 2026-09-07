package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import com.example.domain.manager.VoiceEngineType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiEngine
import com.example.data.model.EmotionalPreset
import com.example.data.model.MusicTrackOption
import com.example.data.model.NarrativeTemplate
import com.example.data.model.TransitionStyle
import com.example.data.model.VoiceActor
import com.example.service.XAiAuthState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.immersiveGradientBackground
import com.example.ui.components.WorkflowProgressCard
import com.example.ui.components.WorkflowStep
import com.example.ui.viewmodel.PromoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreativeDirectionScreen(
    viewModel: PromoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onOpenGrokOAuth: (() -> Unit)? = null
) {
    val brandProfile by viewModel.brandProfile.collectAsState()
    val narrativeTemplate by viewModel.narrativeTemplate.collectAsState()
    val targetDuration by viewModel.targetDurationSeconds.collectAsState()
    val voiceActor by viewModel.voiceActor.collectAsState()
    val musicTrack by viewModel.musicTrack.collectAsState()
    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isVoiceSynthesizing by viewModel.isVoiceSynthesizing.collectAsState()
    val aiEngine by viewModel.aiEngine.collectAsState()
    val authState by viewModel.xaiAuthState.collectAsState()
    val isGrokConnected = authState is XAiAuthState.Connected
    val voiceCapability by viewModel.voiceCapability.collectAsState()
    val creativeBrief by viewModel.creativeBrief.collectAsState()
    val scanManifest by viewModel.scanManifest.collectAsState()

    Box(modifier = Modifier.fillMaxSize().immersiveGradientBackground()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Creative Direction",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Narrative arc, voice audition & transition style",
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
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC050505))
                        .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${narrativeTemplate.title} (${targetDuration}s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Voice: ${voiceActor.voiceName} • ${transitionStyle.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.generateScript()
                                onNavigateToStudio()
                            },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("generate_remotion_scenes_button")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Building Scenes...", color = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Build Remotion Scenes", fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
                    WorkflowProgressCard(
                        viewModel = viewModel,
                        currentStep = WorkflowStep.NARRATIVE,
                        onStepClick = { step ->
                            when (step) {
                                WorkflowStep.IMPORT -> onNavigateBack()
                                WorkflowStep.NARRATIVE -> { /* already here */ }
                                WorkflowStep.VOICEOVER, WorkflowStep.PREVIEW, WorkflowStep.EXPORT -> onNavigateToStudio()
                            }
                        }
                    )
                }

                item {
                    // 0. AI Engine & SuperGrok OAuth Card
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (aiEngine == AiEngine.GROK) Color(0x3310B981) else Color(0x336366F1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (aiEngine == AiEngine.GROK) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (aiEngine == AiEngine.GROK) AccentEmerald else PrimaryLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "AI Scripting Engine",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (aiEngine == AiEngine.GROK) "xAI Grok 2 • SuperGrok Account" else "Google Gemini 2.5 Flash",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (aiEngine == AiEngine.GROK) AccentEmerald else TextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGrokConnected) Color(0x2610B981) else Color(0x1AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isGrokConnected) AccentEmerald else GlassBorder
                                    ),
                                    modifier = Modifier.clickable { onOpenGrokOAuth?.invoke() }
                                ) {
                                    Text(
                                        text = if (isGrokConnected) "SuperGrok OK" else "Connect OAuth",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGrokConnected) AccentEmerald else PrimaryLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Engine selector row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Google Gemini option
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setAiEngine(AiEngine.GEMINI) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (aiEngine == AiEngine.GEMINI) Color(0x336366F1) else Color(0x0AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (aiEngine == AiEngine.GEMINI) PrimaryLight else GlassBorder
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Google Gemini",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Gemini 2.5 Multimodal",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                // xAI Grok option
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isGrokConnected) {
                                                viewModel.setAiEngine(AiEngine.GROK)
                                            } else {
                                                onOpenGrokOAuth?.invoke()
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (aiEngine == AiEngine.GROK) Color(0x3310B981) else Color(0x0AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (aiEngine == AiEngine.GROK) AccentEmerald else GlassBorder
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "xAI Grok 2",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            if (isGrokConnected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(AccentEmerald)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isGrokConnected) "SuperGrok Active" else "Tap to Connect",
                                            fontSize = 10.sp,
                                            color = if (isGrokConnected) AccentEmerald else PrimaryLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STRUCTURED CREATIVE BRIEF & HARD BUDGET CARD
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
                                            .background(Color(0x268B5CF6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "CREATIVE BRIEF & BUDGETS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "Structured Strategic Foundation",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x228B5CF6)
                                ) {
                                    Text(
                                        text = "Deterministic",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Engineered from sanitized repository facts to prevent hallucinated marketing claims.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            // Brief breakdown items
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0DFFFFFF))
                                        .padding(12.dp)
                                 ) {
                                    Column {
                                        Text("TARGET AUDIENCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                        Text(creativeBrief?.targetAudience ?: brandProfile.targetAudience.ifEmpty { "Software Engineers & Builders" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0DFFFFFF))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("CORE PROMISE & VALUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                        Text(creativeBrief?.coreBenefit ?: brandProfile.description.ifEmpty { "Turn repository code into high-converting developer video" }, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x0DFFFFFF))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("TONE KEYWORDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                            Text(
                                                "Modern • Punchy • Fast",
                                                fontSize = 11.sp,
                                                color = AccentCyan
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x0DFFFFFF))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("CALL TO ACTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                            Text(
                                                creativeBrief?.callToAction ?: "Star on GitHub",
                                                fontSize = 11.sp,
                                                color = AccentEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Hard Budget Envelope
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x1A10B981))
                                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ENFORCED PRODUCTION BUDGETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentEmerald, letterSpacing = 1.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration Limit: ${targetDuration}s", fontSize = 11.sp, color = TextSecondary)
                                        Text("Word Budget: ~${(targetDuration * 2.3).toInt()} words", fontSize = 11.sp, color = TextSecondary)
                                        Text("Tokens: < 2,000", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // 1. Narrative Template Glass Card
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "STORYBOARD ARC",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Narrative Architecture",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Proven high-converting video structures engineered for SaaS & developer tools.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            NarrativeTemplate.values().forEach { template ->
                                val isSelected = narrativeTemplate == template
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) Color(0x268B5CF6) else Color(0x0DFFFFFF))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryLight else GlassBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { viewModel.setNarrativeTemplate(template) }
                                        .testTag("narrative_${template.name}")
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setNarrativeTemplate(template) },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryLight)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = template.title,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = template.arc,
                                                fontSize = 11.sp,
                                                color = AccentCyan,
                                                lineHeight = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Best for: ${template.bestFor}",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // 2. Target Video Duration
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3306B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "RUN-TIME TARGET",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Target Video Duration",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(30, "30 Seconds", "Hyper-speed cut (4 scenes)"),
                                    Triple(60, "60 Seconds", "Standard promo (5 scenes)"),
                                    Triple(90, "90 Seconds", "Deep feature demo (6 scenes)")
                                ).forEach { (seconds, label, sub) ->
                                    val isSelected = targetDuration == seconds
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) Color(0x338B5CF6) else Color(0x0DFFFFFF))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) PrimaryLight else GlassBorder,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { viewModel.setTargetDuration(seconds) }
                                            .testTag("duration_${seconds}s_button")
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = sub, fontSize = 9.sp, color = TextMuted, lineHeight = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // 3. Voice Actor & Audition
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3310B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "VOICEOVER AUDITION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    val isEleven = voiceCapability.activeEngine == VoiceEngineType.ELEVENLABS_NEURAL && !voiceCapability.isFallback
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isEleven) "ElevenLabs AI Voice Actor" else "Local Android Speech Engine",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isEleven) Color(0x2E10B981) else Color(0x2EF59E0B)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isEleven) AccentEmerald.copy(alpha = 0.5f) else AccentAmber.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isEleven) AccentEmerald else AccentAmber)
                                                )
                                                Text(
                                                    text = if (isEleven) "HD Neural Active" else "Local Android TTS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEleven) AccentEmerald else AccentAmber
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = voiceCapability.fallbackReason ?: "Studio-grade neural speech generated via ElevenLabs with human inflection and emotion curves.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            VoiceActor.values().forEach { actor ->
                                val isSelected = voiceActor == actor
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) Color(0x2610B981) else Color(0x0DFFFFFF))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) AccentEmerald else GlassBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { viewModel.setVoiceActor(actor) }
                                        .testTag("voice_${actor.name}")
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setVoiceActor(actor) },
                                                colors = RadioButtonDefaults.colors(selectedColor = AccentEmerald)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${actor.voiceName} (${if (actor.isMale) "Male" else "Female"})",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = actor.style,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "Best for: ${actor.bestFor}",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        // Audition button
                                        Button(
                                            onClick = { viewModel.auditionVoice(actor, EmotionalPreset.CONFIDENT) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("audition_voice_${actor.name}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Audition",
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (viewModel.isElevenLabsConfigured) "Audition HD" else "Audition", fontSize = 11.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // 4. Signature Transitions & SFX
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x33CBD5E1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = MetallicSilver,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "TRANSITION STYLE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "Scene Transitions & SFX",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Test Metallic Swoosh SFX button
                                Button(
                                    onClick = { viewModel.playMetallicSwoosh() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("test_swoosh_sfx_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = MetallicSilver,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Swoosh SFX", fontSize = 11.sp, color = MetallicSilver)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            TransitionStyle.values().forEach { style ->
                                val isSelected = transitionStyle == style
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) Color(0x26CBD5E1) else Color(0x0DFFFFFF))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MetallicSilver else GlassBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            viewModel.setTransitionStyle(style)
                                            if (style == TransitionStyle.METALLIC_SWOOSH) viewModel.playMetallicSwoosh()
                                        }
                                        .testTag("transition_${style.name}")
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setTransitionStyle(style) },
                                            colors = RadioButtonDefaults.colors(selectedColor = MetallicSilver)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = style.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = style.description,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // 5. Soundtrack Selection
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x338B5CF6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SecondaryLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SOUNDTRACK AUDIO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Background Soundtrack",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            MusicTrackOption.values().forEach { track ->
                                val isSelected = musicTrack == track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) Color(0x268B5CF6) else Color(0x0DFFFFFF))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryLight else GlassBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { viewModel.setMusicTrack(track) }
                                        .testTag("track_${track.name}")
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setMusicTrack(track) },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryLight)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = track.trackTitle + if (track.bpm > 0) " (${track.bpm} BPM)" else "",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = track.vibe,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
