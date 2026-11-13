package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiVideoModel
import com.example.data.model.AspectRatioOption
import com.example.data.model.AudioTrackOption
import com.example.data.model.CameraMovement
import com.example.data.model.PromptLanguage
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoGenMode
import com.example.data.model.VisualStyle
import com.example.media.StudioSpeechAndAudioManager
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GradientStudioButton
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioOptionChip
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.AiToolsViewModel
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioCreateScreen(
    studioViewModel: StudioViewModel,
    aiToolsViewModel: AiToolsViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val form by studioViewModel.formState.collectAsState()
    val profileState by studioViewModel.userProfile.collectAsState()
    val projectsState by studioViewModel.projects.collectAsState()
    val backendHealth by studioViewModel.backendHealth.collectAsState()
    val isSubmitting by studioViewModel.isSubmittingJob.collectAsState()
    val promptAssistState by aiToolsViewModel.promptResultState.collectAsState()
    val negAssistState by aiToolsViewModel.negativePromptState.collectAsState()
    val activeMusic by audioManager.activeMusicTrack.collectAsState()

    var configDialogMessage by remember { mutableStateOf<String?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            studioViewModel.setReferenceMediaFromUri(context, uri)
        }
    }

    val currentProfile: UserProfile? = (profileState as? UiState.Success)?.data
    val projectList: List<StudioProject> = (projectsState as? UiState.Success)?.data.orEmpty()
    val selectedProject = projectList.firstOrNull { it.id == form.selectedProjectId }

    val providerConfigured = backendHealth.health?.providers?.get(form.selectedModel.displayName)
    val showConfigBanner = !backendHealth.isConfigured || !backendHealth.isReachable || providerConfigured == false

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Studio Header Banner
            GlassCard(accentHighlight = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "VASAN AI STUDIO",
                                style = MaterialTheme.typography.titleLarge,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioYellow
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioYellow.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioYellow.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "Credits Balance",
                                tint = StudioYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${currentProfile?.credits ?: 0} cr",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = StudioYellow
                            )
                        }
                    }
                }
            }

            // Clear API Configuration Banner (Strict no-fake-video policy)
            AnimatedVisibility(visible = showConfigBanner) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentHighlight = false
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Configuration Required",
                            tint = StudioWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "API Configuration Notice",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioWarning
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val bannerText = when {
                        !backendHealth.isConfigured ->
                            "Node.js Express backend URL is not set. Video generation uses real APIs (Kling, MiniMax, Veo, Runway) via /backend and never fakes output. Configure your backend URL in Settings."
                        !backendHealth.isReachable ->
                            backendHealth.message
                        else -> {
                            val missing = backendHealth.health?.missingEnvByProvider
                                ?.get(form.selectedModel.displayName)
                                ?.joinToString(", ")
                                ?: form.selectedModel.envKeys.joinToString(", ")
                            "${form.selectedModel.displayName} API credentials ($missing) are missing in /backend/.env."
                        }
                    }
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        border = BorderStroke(1.dp, StudioYellow.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("open_backend_settings_button")
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Configure Backend & Keys", color = StudioYellow)
                    }
                }
            }

            // AI Model Selector
            GlassCard {
                SectionHeader(
                    title = "1. Select AI Video Model",
                    subtitle = "Choose engine routed securely through your Express backend"
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AiVideoModel.entries.forEach { model ->
                        val isSelected = form.selectedModel == model
                        val isReady = backendHealth.health?.providers?.get(model.displayName) == true
                        Surface(
                            onClick = { studioViewModel.updateForm { it.copy(selectedModel = model) } },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) StudioPurple.copy(alpha = 0.24f) else StudioSurfaceElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) StudioYellow else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_card_${model.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = model.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) StudioYellow else StudioTextPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        StudioStatusBadge(
                                            text = model.badge,
                                            color = StudioPurpleGlow
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = model.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StudioTextSecondary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                StudioStatusBadge(
                                    text = if (isReady) "API Ready" else "${model.baseCredits}+ cr",
                                    color = if (isReady) StudioSuccess else StudioTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Generation Mode & Media Upload
            GlassCard {
                SectionHeader(
                    title = "2. Generation Mode",
                    subtitle = "Text, Image, Video-to-Video, Cartoon & Character Animation"
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoGenMode.entries.forEach { mode ->
                        StudioOptionChip(
                            label = mode.displayName,
                            selected = form.selectedMode == mode,
                            onClick = { studioViewModel.updateForm { it.copy(selectedMode = mode) } }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val type = if (form.selectedMode == VideoGenMode.VIDEO_TO_VIDEO) {
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            } else {
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            }
                            mediaPicker.launch(PickVisualMediaRequest(type))
                        },
                        border = BorderStroke(1.dp, StudioPurpleGlow.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("attach_reference_media_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Attach Reference Media",
                            tint = StudioPurpleGlow
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = form.referenceMediaLabel ?: "Attach Reference Image / Frame",
                            color = StudioTextPrimary
                        )
                    }

                    if (form.referenceMediaBytes != null) {
                        IconButton(onClick = { studioViewModel.clearReferenceMedia() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear Reference Media",
                                tint = StudioError
                            )
                        }
                    }
                }
            }

            // Prompt & AI Assistant
            GlassCard {
                SectionHeader(
                    title = "3. Creative Prompt & Character",
                    subtitle = "English & Tamil support with Gemini AI prompt tools",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PromptLanguage.entries.forEach { lang ->
                                StudioOptionChip(
                                    label = lang.label,
                                    selected = form.language == lang,
                                    onClick = { studioViewModel.updateForm { it.copy(language = lang) } }
                                )
                            }
                        }
                    }
                )

                if (projectList.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Project / Character Consistency:",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioTextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioOptionChip(
                            label = "Standalone",
                            selected = form.selectedProjectId == null,
                            onClick = { studioViewModel.updateForm { it.copy(selectedProjectId = null) } }
                        )
                        projectList.forEach { proj ->
                            StudioOptionChip(
                                label = proj.title,
                                selected = form.selectedProjectId == proj.id,
                                onClick = { studioViewModel.applyProjectToStudio(proj) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.prompt,
                    onValueChange = { text -> studioViewModel.updateForm { it.copy(prompt = text) } },
                    label = { Text("Describe your scene (${form.language.label})") },
                    placeholder = {
                        Text(
                            if (form.language == PromptLanguage.TAMIL) {
                                "உதாரணம்: சென்னை மெரினா கடற்கரையில் பொன்னிற சூரிய உதயம், சினிமா ட்ரோன் காட்சி..."
                            } else {
                                "e.g., Futuristic cyber-noir street in rain, golden neon reflections, slow dolly shot..."
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_prompt_input"),
                    colors = studioTextFieldColors()
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            aiToolsViewModel.generateOrImprovePrompt(
                                input = form.prompt,
                                language = form.language,
                                style = form.visualStyle.label,
                                characterSeed = selectedProject?.characterSeed.orEmpty(),
                                mode = form.selectedMode.displayName,
                                isImprove = form.prompt.isNotBlank()
                            ) { improved ->
                                studioViewModel.updateForm { it.copy(prompt = improved) }
                            }
                        },
                        enabled = promptAssistState !is UiState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_improve_prompt_button"),
                        border = BorderStroke(1.dp, StudioYellow.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (form.prompt.isBlank()) "AI Generate" else "AI Improve",
                            color = StudioYellow
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            aiToolsViewModel.generateNegativePrompt(
                                prompt = form.prompt.ifBlank { form.visualStyle.label },
                                style = form.visualStyle.label
                            ) { neg ->
                                studioViewModel.updateForm { it.copy(negativePrompt = neg) }
                            }
                        },
                        enabled = negAssistState !is UiState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_negative_prompt_button"),
                        border = BorderStroke(1.dp, StudioPurpleGlow.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = StudioPurpleGlow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Negative", color = StudioPurpleGlow)
                    }
                }

                if (promptAssistState is UiState.Loading || negAssistState is UiState.Loading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = StudioYellow,
                        trackColor = StudioSurfaceElevated
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.negativePrompt,
                    onValueChange = { text -> studioViewModel.updateForm { it.copy(negativePrompt = text) } },
                    label = { Text("Negative Prompt (Exclusions)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("negative_prompt_input"),
                    colors = studioTextFieldColors()
                )
            }

            // Video Settings
            GlassCard {
                SectionHeader(
                    title = "4. Video & Camera Settings",
                    subtitle = "Duration, aspect ratio, resolution, FPS, camera & audio"
                )
                Spacer(Modifier.height(12.dp))

                Text("Duration", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { sec ->
                        StudioOptionChip(
                            label = "${sec}s",
                            selected = form.durationSeconds == sec,
                            onClick = { studioViewModel.updateForm { it.copy(durationSeconds = sec) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        StudioOptionChip(
                            label = ratio.label,
                            selected = form.aspectRatio == ratio,
                            onClick = { studioViewModel.updateForm { it.copy(aspectRatio = ratio) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Resolution", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("720p", "1080p").forEach { res ->
                                StudioOptionChip(
                                    label = res,
                                    selected = form.resolution == res,
                                    onClick = { studioViewModel.updateForm { it.copy(resolution = res) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frame Rate", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24, 30, 60).forEach { fps ->
                                StudioOptionChip(
                                    label = "$fps",
                                    selected = form.fps == fps,
                                    onClick = { studioViewModel.updateForm { it.copy(fps = fps) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Visual Style", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisualStyle.entries.forEach { style ->
                        StudioOptionChip(
                            label = style.label,
                            selected = form.visualStyle == style,
                            onClick = { studioViewModel.updateForm { it.copy(visualStyle = style) } }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Camera Movement", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CameraMovement.entries.forEach { cam ->
                        StudioOptionChip(
                            label = cam.label,
                            selected = form.cameraMovement == cam,
                            onClick = { studioViewModel.updateForm { it.copy(cameraMovement = cam) } }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Motion Intensity", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                    Text(
                        text = "${form.motionIntensity} / 10",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioYellow
                    )
                }
                Slider(
                    value = form.motionIntensity.toFloat(),
                    onValueChange = { v -> studioViewModel.updateForm { it.copy(motionIntensity = v.toInt()) } },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioYellow,
                        activeTrackColor = StudioYellow,
                        inactiveTrackColor = StudioSurfaceElevated
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text("Background Music & Audio", style = MaterialTheme.typography.labelLarge, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioTrackOption.entries.forEach { track ->
                        val isPlaying = activeMusic == track
                        StudioOptionChip(
                            label = track.label,
                            selected = form.audioTrack == track,
                            icon = when {
                                isPlaying -> Icons.Rounded.Stop
                                track == AudioTrackOption.VOICEOVER_TTS -> Icons.Rounded.GraphicEq
                                track != AudioTrackOption.NONE -> Icons.Rounded.MusicNote
                                else -> null
                            },
                            onClick = {
                                studioViewModel.updateForm { it.copy(audioTrack = track) }
                                if (track != AudioTrackOption.NONE && track != AudioTrackOption.VOICEOVER_TTS) {
                                    audioManager.toggleMusicPreview(track, scope)
                                } else {
                                    audioManager.stopMusicPreview()
                                }
                            }
                        )
                    }
                }
            }

            // Generate CTA
            GradientStudioButton(
                text = "Generate Video (${form.estimatedCredits} Credits)",
                icon = Icons.Rounded.MovieCreation,
                isLoading = isSubmitting,
                onClick = {
                    studioViewModel.submitVideoGeneration { msg ->
                        configDialogMessage = msg
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_video_button")
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (configDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { configDialogMessage = null },
            containerColor = StudioSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StudioWarning)
                    Spacer(Modifier.width(8.dp))
                    Text("API Configuration Required", color = StudioTextPrimary)
                }
            },
            text = {
                Text(
                    text = configDialogMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configDialogMessage = null
                        onNavigateToSettings()
                    }
                ) {
                    Text("Open Settings", color = StudioYellow)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configDialogMessage = null
                        onNavigateToGallery()
                    }
                ) {
                    Text("View History", color = StudioPurpleGlow)
                }
            }
        )
    }
}

@Composable
fun studioTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StudioYellow,
    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
    focusedLabelColor = StudioYellow,
    unfocusedLabelColor = StudioTextSecondary,
    focusedTextColor = StudioTextPrimary,
    unfocusedTextColor = StudioTextPrimary,
    cursorColor = StudioYellow,
    focusedContainerColor = StudioSurfaceElevated.copy(alpha = 0.65f),
    unfocusedContainerColor = StudioSurfaceElevated.copy(alpha = 0.45f)
)
