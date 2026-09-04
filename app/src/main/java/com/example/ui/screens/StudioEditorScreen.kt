package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.model.EmotionalPreset
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
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
import com.example.ui.viewmodel.PromoViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudioEditorScreen(
    viewModel: PromoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExport: () -> Unit,
    onOpenDirectorChat: () -> Unit
) {
    val brandProfile by viewModel.brandProfile.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val activeSceneIndex by viewModel.activeSceneIndex.collectAsState()
    val activeFormat by viewModel.activeAspectRatio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val voiceActor by viewModel.voiceActor.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isVoiceSynthesizing by viewModel.isVoiceSynthesizing.collectAsState()

    val currentScene = scenes.getOrNull(activeSceneIndex)

    Box(modifier = Modifier.fillMaxSize().immersiveGradientBackground()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${brandProfile.name} Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Timeline, Voiceover & 16:9/9:16 Canvas",
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
                    actions = {
                        IconButton(
                            onClick = { viewModel.saveCurrentProject() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2210B981))
                                .testTag("save_project_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Project",
                                tint = AccentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenDirectorChat,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x33F59E0B))
                                .testTag("open_chat_topbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Director",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
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
                        val totalDuration = scenes.sumOf { it.durationSeconds.toDouble() }
                        Column {
                            Text(
                                text = "${scenes.size} Scenes • ${String.format("%.1f", totalDuration)}s Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Format: ${activeFormat.label} (${activeFormat.width}×${activeFormat.height})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = onNavigateToExport,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("export_promo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export & Render", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    // THE LIVE VIDEO PLAYER VIEWPORT
                    VideoPlayerView(
                        scenes = scenes,
                        brandName = brandProfile.name,
                        primaryColorHex = brandProfile.primaryColorHex,
                        secondaryColorHex = brandProfile.secondaryColorHex,
                        accentColorHex = brandProfile.accentColorHex,
                        activeFormat = activeFormat,
                        onFormatToggle = { viewModel.setAspectRatio(it) },
                        onSceneChanged = { viewModel.onSceneChanged(it) },
                        onTriggerSwoosh = { viewModel.playMetallicSwoosh() },
                        isPlaying = isPlaying,
                        onPlayPauseToggle = { viewModel.togglePlayback(it) },
                        modifier = Modifier.testTag("interactive_video_canvas")
                    )
                }

                // HORIZONTAL SCENE TIMELINE STRIP
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCENE TIMELINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = TextMuted
                            )

                            // Add Scene Button
                            Button(
                                onClick = {
                                    val newScene = PromoScene(
                                        orderIndex = scenes.size,
                                        title = "New Feature",
                                        subtitle = "Seamless Automation",
                                        voiceover = "Fast, effortless, and fully integrated with your workflow.",
                                        emotionalPreset = EmotionalPreset.CONFIDENT,
                                        durationSeconds = 3.5f,
                                        visualType = SceneVisualType.FEATURE_SPOTLIGHT,
                                        accentTag = "FEATURE"
                                    )
                                    viewModel.addScene(newScene)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_scene_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Scene", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(scenes) { index, scene ->
                                val isSelected = index == activeSceneIndex
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0x268B5CF6) else GlassBackground)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryLight else GlassBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable { viewModel.onSceneChanged(index) }
                                        .testTag("timeline_scene_$index")
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) PrimaryLight else TextMuted
                                            )
                                            Text(
                                                text = "${scene.durationSeconds}s",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = scene.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (scene.emotionalPreset) {
                                                EmotionalPreset.RAGE -> AccentRose.copy(alpha = 0.2f)
                                                EmotionalPreset.WHISPER -> AccentCyan.copy(alpha = 0.2f)
                                                EmotionalPreset.CONFIDENT -> PrimaryIndigo.copy(alpha = 0.25f)
                                                EmotionalPreset.WARM -> Color(0x3310B981)
                                                EmotionalPreset.DRAMATIC -> AccentAmber.copy(alpha = 0.2f)
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when (scene.emotionalPreset) {
                                                    EmotionalPreset.RAGE -> AccentRose.copy(alpha = 0.5f)
                                                    EmotionalPreset.WHISPER -> AccentCyan.copy(alpha = 0.5f)
                                                    EmotionalPreset.CONFIDENT -> PrimaryLight.copy(alpha = 0.5f)
                                                    EmotionalPreset.WARM -> Color(0x8810B981)
                                                    EmotionalPreset.DRAMATIC -> AccentAmber.copy(alpha = 0.5f)
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = scene.emotionalPreset.label.split(" ").first(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (scene.emotionalPreset) {
                                                    EmotionalPreset.RAGE -> AccentRose
                                                    EmotionalPreset.WHISPER -> AccentCyan
                                                    EmotionalPreset.CONFIDENT -> PrimaryLight
                                                    EmotionalPreset.WARM -> AccentEmerald
                                                    EmotionalPreset.DRAMATIC -> AccentAmber
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SCENE DETAIL INSPECTOR FOR CURRENT SCENE
                if (currentScene != null) {
                    item {
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
                                                .background(Color(0x338B5CF6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = null,
                                                tint = SecondaryLight,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "INSPECTOR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.5.sp,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = "Scene ${activeSceneIndex + 1} Configuration",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (scenes.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteScene(activeSceneIndex) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x22F43F5E))
                                                .testTag("delete_current_scene_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Scene",
                                                tint = AccentRose,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Scene Title & Subtitle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Title", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = currentScene.title,
                                            onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(title = it)) },
                                            modifier = Modifier.fillMaxWidth().testTag("scene_title_field"),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryLight,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0x1AFFFFFF),
                                                unfocusedContainerColor = Color(0x0DFFFFFF)
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Subtitle", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = currentScene.subtitle,
                                            onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(subtitle = it)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryLight,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0x1AFFFFFF),
                                                unfocusedContainerColor = Color(0x0DFFFFFF)
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Visual Type Selection
                                Text("Visual Layout Type", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SceneVisualType.values().forEach { vType ->
                                        val isSelected = currentScene.visualType == vType
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0x338B5CF6) else Color(0x0DFFFFFF),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) PrimaryLight else GlassBorder
                                            ),
                                            modifier = Modifier.clickable {
                                                viewModel.updateScene(activeSceneIndex, currentScene.copy(visualType = vType))
                                            }
                                        ) {
                                            Text(
                                                text = vType.displayName,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // VOICEOVER NARRATION & EMOTION
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Voiceover Narration Script", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        if (viewModel.isElevenLabsConfigured) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x2610B981))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(AccentEmerald)
                                                    )
                                                    Text("ElevenLabs HD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                                }
                                            }
                                        }
                                    }

                                    // Audition Narration Button (Plays actual scene voiceover or quote)
                                    Button(
                                        onClick = {
                                            if (currentScene.voiceover.isNotBlank()) {
                                                viewModel.speakSceneText(currentScene.voiceover, voiceActor, currentScene.emotionalPreset)
                                            } else {
                                                viewModel.auditionVoice(voiceActor, currentScene.emotionalPreset)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoiceSynthesizing) AccentEmerald else GlassBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("audition_narration_button")
                                    ) {
                                        if (isVoiceSynthesizing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 2.dp,
                                                color = AccentEmerald
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Synthesizing...", fontSize = 11.sp, color = AccentEmerald)
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = if (viewModel.isElevenLabsConfigured) AccentEmerald else AccentAmber,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (viewModel.isElevenLabsConfigured) "Audition HD" else "Audition",
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = currentScene.voiceover,
                                    onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(voiceover = it)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .testTag("scene_voiceover_field"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryLight,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0x1AFFFFFF),
                                        unfocusedContainerColor = Color(0x0DFFFFFF)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Emotional Preset Selector for Scene
                                Text("Narration Emotion Arc", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EmotionalPreset.values().forEach { preset ->
                                        val isSelected = currentScene.emotionalPreset == preset
                                        val badgeColor = when (preset) {
                                            EmotionalPreset.RAGE -> AccentRose
                                            EmotionalPreset.WHISPER -> AccentCyan
                                            EmotionalPreset.CONFIDENT -> PrimaryLight
                                            EmotionalPreset.WARM -> AccentEmerald
                                            EmotionalPreset.DRAMATIC -> AccentAmber
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) badgeColor.copy(alpha = 0.25f) else Color(0x0DFFFFFF),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) badgeColor else GlassBorder
                                            ),
                                            modifier = Modifier.clickable {
                                                viewModel.updateScene(activeSceneIndex, currentScene.copy(emotionalPreset = preset))
                                            }
                                        ) {
                                            Text(
                                                text = preset.label,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Duration Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scene Duration", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(
                                        text = "${String.format("%.1f", currentScene.durationSeconds)} seconds",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Slider(
                                    value = currentScene.durationSeconds,
                                    onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(durationSeconds = it)) },
                                    valueRange = 2.0f..6.0f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryLight,
                                        activeTrackColor = PrimaryLight,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )

                                // Specific fields based on visual type
                                if (currentScene.visualType == SceneVisualType.CODE_TERMINAL) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Code Snippet (Terminal display)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = currentScene.codeSnippet ?: "",
                                        onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(codeSnippet = it)) },
                                        modifier = Modifier.fillMaxWidth().height(88.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryLight,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = AccentCyan,
                                            unfocusedTextColor = AccentCyan,
                                            focusedContainerColor = Color(0xFF030712),
                                            unfocusedContainerColor = Color(0xFF030712)
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    )
                                } else if (currentScene.visualType == SceneVisualType.METRIC_COUNTER) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Metric Number", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = currentScene.metricNumber ?: "50,000+",
                                                onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(metricNumber = it)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryLight,
                                                    unfocusedBorderColor = GlassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0x1AFFFFFF),
                                                    unfocusedContainerColor = Color(0x0DFFFFFF)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text("Metric Label", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = currentScene.metricLabel ?: "Videos Rendered Worldwide",
                                                onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(metricLabel = it)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryLight,
                                                    unfocusedBorderColor = GlassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0x1AFFFFFF),
                                                    unfocusedContainerColor = Color(0x0DFFFFFF)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }
                                } else if (currentScene.visualType == SceneVisualType.LOGO_REVEAL_CTA) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("CTA Button Text", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = currentScene.ctaButtonText ?: "Install Skill",
                                                onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(ctaButtonText = it)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryLight,
                                                    unfocusedBorderColor = GlassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0x1AFFFFFF),
                                                    unfocusedContainerColor = Color(0x0DFFFFFF)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("CTA Link URL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = currentScene.ctaUrl ?: "https://fastsolve.app",
                                                onValueChange = { viewModel.updateScene(activeSceneIndex, currentScene.copy(ctaUrl = it)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PrimaryLight,
                                                    unfocusedBorderColor = GlassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedContainerColor = Color(0x1AFFFFFF),
                                                    unfocusedContainerColor = Color(0x0DFFFFFF)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Veo 3 Video Generator Button
                                Button(
                                    onClick = { viewModel.generateVeoVideoForScene(activeSceneIndex) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_veo_clip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoCameraBack,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate Veo 3 Video Clip for Scene", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
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
