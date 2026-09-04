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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
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
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x2210B981)
                                        ) {
                                            Text(
                                                text = "READY TO RENDER",
                                                fontSize = 9.sp,
                                                color = AccentEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
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
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x2210B981)
                                        ) {
                                            Text(
                                                text = "READY TO RENDER",
                                                fontSize = 9.sp,
                                                color = AccentEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
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
                                            Column {
                                                Text(
                                                    text = "Render Completed (1080p 60fps)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Saved to ./out/landscape.mp4 and ./out/portrait.mp4",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA7F3D0)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
