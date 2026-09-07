package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PromoViewModel

enum class WorkflowStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val stepNumber: Int
) {
    IMPORT("Project Import", "Codebase ingestion", Icons.Default.Download, 1),
    NARRATIVE("Narrative", "Story arc & script", Icons.Default.AutoAwesome, 2),
    VOICEOVER("Voiceover", "HD neural voices", Icons.Default.Mic, 3),
    PREVIEW("Video Preview", "Dual-aspect canvas", Icons.Default.Movie, 4),
    EXPORT("Export", "Remotion & MP4", Icons.Default.VideoFile, 5)
}

/**
 * High-craft visual Workflow Progress Card
 * Tracks the 5 requested core capabilities:
 * 1. Project Importer
 * 2. Narrative Generator
 * 3. Voiceover Integration
 * 4. Video Preview Layout
 * 5. Progress Indicator
 */
@Composable
fun WorkflowProgressCard(
    viewModel: PromoViewModel,
    currentStep: WorkflowStep,
    onStepClick: (WorkflowStep) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val brandProfile by viewModel.brandProfile.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgressPercent.collectAsState()
    val scanStage by viewModel.scanStageText.collectAsState()

    val isGenerating by viewModel.isGenerating.collectAsState()
    val narrativeProgress by viewModel.narrativeProgressPercent.collectAsState()
    val narrativeStage by viewModel.narrativeStageText.collectAsState()

    val isVoiceSynthesizing by viewModel.isVoiceSynthesizing.collectAsState()
    val voiceProgress by viewModel.voiceSynthesisProgressPercent.collectAsState()
    val voiceStage by viewModel.voiceSynthesisStageText.collectAsState()

    val activeRenderJobs by viewModel.activeRenderJobs.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_wf")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Calculate step statuses
    val isImportDone = brandProfile.name.isNotBlank() && brandProfile.name != "My Application"
    val isNarrativeDone = scenes.isNotEmpty()
    val isVoiceoverConfigured = scenes.any { it.voiceover.isNotBlank() }
    val isPreviewReady = scenes.isNotEmpty()

    val overallPercent = when {
        activeRenderJobs.any { it.status == "COMPLETED" } -> 100
        isVoiceoverConfigured && isPreviewReady -> 80
        isNarrativeDone -> 60
        isImportDone -> 40
        else -> 20
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("workflow_progress_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x266366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PRODUCTION WORKFLOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isScanning || isGenerating || isVoiceSynthesizing) AccentCyan else AccentEmerald)
                            )
                        }
                        Text(
                            text = when (currentStep) {
                                WorkflowStep.IMPORT -> "Step 1: Codebase Discovery & Import"
                                WorkflowStep.NARRATIVE -> "Step 2: Narrative Generation & Storyboard"
                                WorkflowStep.VOICEOVER -> "Step 3: Neural Voiceover Integration"
                                WorkflowStep.PREVIEW -> "Step 4: Dual-Format Video Preview"
                                WorkflowStep.EXPORT -> "Step 5: Remotion & MP4 Export"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Progress Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$overallPercent% Ready",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Step Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkflowStep.entries.forEachIndexed { index, step ->
                    val isDone = when (step) {
                        WorkflowStep.IMPORT -> isImportDone
                        WorkflowStep.NARRATIVE -> isNarrativeDone
                        WorkflowStep.VOICEOVER -> isVoiceoverConfigured
                        WorkflowStep.PREVIEW -> isPreviewReady
                        WorkflowStep.EXPORT -> activeRenderJobs.any { it.status == "COMPLETED" }
                    }
                    val isCurrent = step == currentStep
                    val isStepActive = (step == WorkflowStep.IMPORT && isScanning) ||
                            (step == WorkflowStep.NARRATIVE && isGenerating) ||
                            (step == WorkflowStep.VOICEOVER && isVoiceSynthesizing)

                    StepNode(
                        step = step,
                        isDone = isDone,
                        isCurrent = isCurrent,
                        isActive = isStepActive,
                        pulseAlpha = if (isStepActive) pulseAlpha else 1f,
                        onClick = { onStepClick(step) }
                    )

                    // Divider line between steps
                    if (index < WorkflowStep.entries.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else BorderSubtle
                                )
                        )
                    }
                }
            }

            // Live Dynamic Progress Sub-Bar (Active when background task is running)
            AnimatedVisibility(visible = isScanning || isGenerating || isVoiceSynthesizing) {
                val (stageTitle, progressVal) = when {
                    isScanning -> (scanStage.ifEmpty { "Scanning repository structure..." }) to scanProgress
                    isGenerating -> (narrativeStage.ifEmpty { "Generating narrative script..." }) to narrativeProgress
                    isVoiceSynthesizing -> (voiceStage.ifEmpty { "Synthesizing scene voiceover..." }) to voiceProgress
                    else -> "Processing..." to 50
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$progressVal%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepNode(
    step: WorkflowStep,
    isDone: Boolean,
    isCurrent: Boolean,
    isActive: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val bgColor = when {
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
        isCurrent -> MaterialTheme.colorScheme.primary
        isDone -> AccentEmerald
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val iconTint = when {
        isActive || isCurrent || isDone -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) Color.White.copy(alpha = 0.8f) else BorderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone && !isCurrent && !isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = step.title,
            fontSize = 9.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
