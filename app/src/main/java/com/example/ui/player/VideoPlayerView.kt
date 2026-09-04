package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioFormat
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeFunction
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeVariable
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    scenes: List<PromoScene>,
    brandName: String,
    primaryColorHex: String,
    secondaryColorHex: String,
    accentColorHex: String,
    activeFormat: AspectRatioFormat,
    onFormatToggle: (AspectRatioFormat) -> Unit,
    onSceneChanged: (Int) -> Unit = {},
    onTriggerSwoosh: () -> Unit = {},
    isPlaying: Boolean,
    onPlayPauseToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (scenes.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No scenes configured yet. Generate or add scenes to preview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    var currentSceneIndex by remember { mutableIntStateOf(0) }
    var sceneProgress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = remember(primaryColorHex) {
        try { Color(android.graphics.Color.parseColor(primaryColorHex)) } catch (_: Exception) { PrimaryIndigo }
    }
    val secondaryColor = remember(secondaryColorHex) {
        try { Color(android.graphics.Color.parseColor(secondaryColorHex)) } catch (_: Exception) { SecondaryViolet }
    }

    // Metallic swoosh animation state
    val swooshOffset = remember { Animatable(-1.5f) }

    // Playback ticker
    val activeScene = scenes.getOrNull(currentSceneIndex) ?: scenes.first()
    val sceneDuration = activeScene.durationSeconds

    LaunchedEffect(isPlaying, currentSceneIndex, scenes) {
        if (!isPlaying) return@LaunchedEffect

        val intervalMs = 33L // ~30 FPS ticker
        val step = intervalMs / (sceneDuration * 1000f)

        while (isPlaying) {
            delay(intervalMs)
            sceneProgress += step
            if (sceneProgress >= 1f) {
                sceneProgress = 0f
                val nextIdx = (currentSceneIndex + 1) % scenes.size
                currentSceneIndex = nextIdx
                onSceneChanged(nextIdx)

                // Trigger metallic swoosh transition
                onTriggerSwoosh()
                swooshOffset.snapTo(-1.2f)
                swooshOffset.animateTo(
                    targetValue = 2.2f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(com.example.ui.theme.GlassBackground)
            .border(1.dp, com.example.ui.theme.GlassBorder, RoundedCornerShape(32.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar: Aspect Ratio Switcher & Format Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x268B5CF6),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x668B5CF6))
                ) {
                    Text(
                        text = "LIVE REMOTION CANVAS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC4B5FD),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scene ${currentSceneIndex + 1}/${scenes.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // Aspect Ratio Toggle: 16:9 vs 9:16
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x14FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GlassBorder)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    val is169 = activeFormat == AspectRatioFormat.LANDSCAPE_16_9
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (is169) PrimaryIndigo else Color.Transparent)
                            .clickable { onFormatToggle(AspectRatioFormat.LANDSCAPE_16_9) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("ratio_16_9_button")
                    ) {
                        Text(
                            text = "16:9 Landscape",
                            fontSize = 12.sp,
                            fontWeight = if (is169) FontWeight.Bold else FontWeight.Medium,
                            color = if (is169) Color.White else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!is169) PrimaryIndigo else Color.Transparent)
                            .clickable { onFormatToggle(AspectRatioFormat.PORTRAIT_9_16) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("ratio_9_16_button")
                    ) {
                        Text(
                            text = "9:16 Portrait",
                            fontSize = 12.sp,
                            fontWeight = if (!is169) FontWeight.Bold else FontWeight.Medium,
                            color = if (!is169) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // THE VIDEO CANVAS VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (activeFormat == AspectRatioFormat.LANDSCAPE_16_9) 210.dp else 340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF030712))
                .border(1.dp, com.example.ui.theme.GlassBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Scene Rendered Content based on active visual type
            RenderSceneVisual(
                scene = activeScene,
                brandName = brandName,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isPortrait = activeFormat == AspectRatioFormat.PORTRAIT_9_16,
                progress = sceneProgress
            )

            // METALLIC SWOOSH OVERLAY EFFECT
            if (swooshOffset.value in -1f..2f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = size.width * swooshOffset.value
                            rotationZ = -18f
                        }
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x30FFFFFF),
                                        Color(0xBBFFFFFF),
                                        Color(0x30FFFFFF),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width * 0.45f, 0f)
                                )
                            )
                        }
                )
            }

            // KARAOKE SUBTITLE BAR AT BOTTOM
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0B0F19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Voice emotional indicator pill
                        Badge(
                            containerColor = when (activeScene.emotionalPreset) {
                                com.example.data.model.EmotionalPreset.RAGE -> AccentRose
                                com.example.data.model.EmotionalPreset.WHISPER -> AccentCyan
                                com.example.data.model.EmotionalPreset.CONFIDENT -> PrimaryIndigo
                                com.example.data.model.EmotionalPreset.WARM -> Color(0xFF10B981)
                                com.example.data.model.EmotionalPreset.DRAMATIC -> AccentAmber
                            }
                        ) {
                            Text(
                                text = activeScene.emotionalPreset.label.split(" ").first(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Subtitle text with progressive highlight
                        HighlightedSubtitleText(
                            text = activeScene.voiceover,
                            progress = sceneProgress
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PROGRESS SCRUBBER BAR
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${activeScene.title} (${activeScene.visualType.displayName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val currentSec = (sceneProgress * sceneDuration).toInt()
                val totalSec = sceneDuration.toInt()
                val frame = (sceneProgress * sceneDuration * 30).toInt()
                Text(
                    text = "Frame $frame | 00:0${currentSec} / 00:0${totalSec} @ 30fps",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Multi-segment timeline bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1F2937)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                scenes.forEachIndexed { index, sc ->
                    val segmentFraction = 1f / scenes.size
                    val isDone = index < currentSceneIndex
                    val isCurrent = index == currentSceneIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isDone -> primaryColor
                                    isCurrent -> primaryColor.copy(alpha = 0.3f)
                                    else -> Color(0xFF374151)
                                }
                            )
                    ) {
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = sceneProgress.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(primaryColor)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CONTROLLER ROW (Rewind, Play/Pause, Fast Forward, Replay, Swoosh button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Manual Metallic Swoosh trigger button
            Button(
                onClick = {
                    onTriggerSwoosh()
                    coroutineScope.launch {
                        swooshOffset.snapTo(-1.2f)
                        swooshOffset.animateTo(
                            targetValue = 2.2f,
                            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.GlassBorder),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("swoosh_sfx_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Metallic Swoosh",
                    tint = MetallicSilver,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Metallic Swoosh", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MetallicSilver)
            }

            // Center playback controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        sceneProgress = 0f
                        currentSceneIndex = if (currentSceneIndex > 0) currentSceneIndex - 1 else scenes.lastIndex
                        onSceneChanged(currentSceneIndex)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous Scene",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Play / Pause Circle
                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onPlayPauseToggle(!isPlaying) }
                        .testTag("play_pause_video_button"),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        sceneProgress = 0f
                        currentSceneIndex = (currentSceneIndex + 1) % scenes.size
                        onSceneChanged(currentSceneIndex)
                        onTriggerSwoosh()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next Scene",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Replay from start
            IconButton(
                onClick = {
                    currentSceneIndex = 0
                    sceneProgress = 0f
                    onSceneChanged(0)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Replay All",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HighlightedSubtitleText(text: String, progress: Float) {
    val words = remember(text) { text.split(" ").filter { it.isNotBlank() } }
    val highlightedWordCount = (progress * words.size).toInt().coerceIn(0, words.size)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        val displayWords = words.take(12) // Keep tidy inside pill
        Text(
            text = displayWords.mapIndexed { idx, word ->
                if (idx < highlightedWordCount) "★$word" else word
            }.joinToString(" ").replace("★", ""),
            fontSize = 11.sp,
            color = if (progress > 0.1f) AccentAmber else Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RenderSceneVisual(
    scene: PromoScene,
    brandName: String,
    primaryColor: Color,
    secondaryColor: Color,
    isPortrait: Boolean,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scene_visual_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    when (scene.visualType) {
        SceneVisualType.HOOK_FRUSTRATION -> {
            // Rage / Frustration Hook: Dark moody vignette, warning shake, bold rage typography
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF3B0712), Color(0xFF0F0407), Color.Black)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scene.accentTag.ifEmpty { "THE RAGE HOOK" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = scene.title,
                        style = if (isPortrait) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.scale(pulseScale)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = scene.subtitle.ifEmpty { "Stop wasting 40 hours on manual demos" },
                        fontSize = 12.sp,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        scene.badgeTags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x221E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44EF4444))
                            ) {
                                Text(
                                    text = "✕ $tag",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        SceneVisualType.CODE_TERMINAL -> {
            // Simulated macOS IDE Terminal with animated code typing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16))
                    .padding(if (isPortrait) 12.dp else 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 0.95f else 0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .shadow(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Terminal Header Bar with Red, Yellow, Green window dots
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "bash — promo-video-skill",
                                fontSize = 10.sp,
                                color = CodeComment,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val snippet = scene.codeSnippet ?: """
                            npx skills add remotion-dev/skills
                            npx skills add AKCodez/promo-video-skill
                            claude "Create a 60s promo video"
                        """.trimIndent()

                        // Simulate typing progress
                        val visibleChars = (progress * snippet.length).toInt().coerceIn(0, snippet.length)
                        val typedText = snippet.take(visibleChars)

                        Text(
                            text = typedText + if ((progress * 10).toInt() % 2 == 0) "▋" else "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        SceneVisualType.BROWSER_MOCKUP_3D -> {
            // 3D Browser Window Mockup with tilted perspective & smooth product preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )
                    .padding(if (isPortrait) 10.dp else 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (isPortrait) 0.95f else 0.88f)
                        .graphicsLayer {
                            rotationY = if (isPortrait) 0f else -6f
                            rotationX = if (isPortrait) 2f else 4f
                            cameraDistance = 12f
                        }
                        .shadow(16.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                ) {
                    Column {
                        // Browser Address Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "🔒 https://${brandName.lowercase().replace(" ", "")}.dev/launch",
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        // Mockup Webpage Body
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B1120))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = scene.title,
                                fontSize = if (isPortrait) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scene.subtitle.ifEmpty { "High performance automated rendering" },
                                fontSize = 11.sp,
                                color = AccentCyan,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                scene.badgeTags.take(3).forEach { badge ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = primaryColor.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SceneVisualType.FEATURE_SPOTLIGHT -> {
            // Feature Spotlight: Neon 3D cards highlighting killer features
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Black)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scene.accentTag.ifEmpty { "CORE CAPABILITY" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = scene.title,
                        style = if (isPortrait) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tags = if (scene.badgeTags.isNotEmpty()) scene.badgeTags else listOf("Fast Render", "Zero Config", "AI Timing")
                        tags.take(3).forEach { tag ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isPortrait) 60.dp else 70.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SceneVisualType.METRIC_COUNTER -> {
            // Social Proof Metric Counter: Rapid counter animation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080C14))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scene.accentTag.ifEmpty { "SCALE & TRACTION" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val rawNumber = scene.metricNumber ?: "50,000+"
                    Text(
                        text = rawNumber,
                        fontSize = if (isPortrait) 38.sp else 46.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = scene.metricLabel ?: "Videos Rendered by Developers Worldwide",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentAmber,
                        trackColor = Color(0xFF1F2937)
                    )
                }
            }
        }

        SceneVisualType.LOGO_REVEAL_CTA -> {
            // Logo Reveal & Call to Action: Starburst particle radial glow and pulsing CTA button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                secondaryColor.copy(alpha = 0.2f),
                                Color.Black
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo Shield
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = primaryColor,
                        modifier = Modifier
                            .size(if (isPortrait) 48.dp else 56.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = brandName,
                        style = if (isPortrait) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Text(
                        text = scene.subtitle.ifEmpty { "Build once. Render dual-format videos forever." },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pulsing CTA Button
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .scale(pulseScale)
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = scene.ctaButtonText ?: "Visit ${brandName.lowercase()}.app",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        SceneVisualType.VEO_AI_VIDEO -> {
            // Veo AI Video Clip Simulation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B14)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Veo 3 AI Video Generation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Aspect Ratio: ${if (isPortrait) "9:16 (Portrait)" else "16:9 (Landscape)"}",
                        fontSize = 11.sp,
                        color = AccentCyan
                    )
                }
            }
        }
    }
}
