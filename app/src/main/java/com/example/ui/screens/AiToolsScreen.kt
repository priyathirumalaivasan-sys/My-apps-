package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioOption
import com.example.data.model.AudioTrackOption
import com.example.data.model.CartoonEpisode
import com.example.data.model.PromptLanguage
import com.example.data.model.ScriptScene
import com.example.data.model.UiState
import com.example.data.model.VideoGenMode
import com.example.media.StudioSpeechAndAudioManager
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GradientStudioButton
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioOptionChip
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.AiToolsViewModel
import com.example.ui.viewmodel.StudioViewModel
import java.io.ByteArrayOutputStream

@Composable
fun AiToolsScreen(
    studioViewModel: StudioViewModel,
    aiToolsViewModel: AiToolsViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onSendToStudio: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf(PromptLanguage.ENGLISH) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "AI Creative Suite",
                subtitle = "Prompts, Tamil/English scripts, Cartoon stories, Images & TTS",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PromptLanguage.entries.forEach { lang ->
                            StudioOptionChip(
                                label = lang.label,
                                selected = language == lang,
                                onClick = { language = lang }
                            )
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Prompt & Script",
                    "Cartoon Story",
                    "AI Image & Thumbnails",
                    "Voiceover & Music"
                ).forEachIndexed { idx, label ->
                    StudioOptionChip(
                        label = label,
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx }
                    )
                }
            }

            when (selectedTab) {
                0 -> PromptAndScriptTab(
                    language = language,
                    aiToolsViewModel = aiToolsViewModel,
                    studioViewModel = studioViewModel,
                    audioManager = audioManager,
                    onSendToStudio = onSendToStudio
                )
                1 -> CartoonStoryTab(
                    language = language,
                    aiToolsViewModel = aiToolsViewModel,
                    studioViewModel = studioViewModel,
                    audioManager = audioManager,
                    onSendToStudio = onSendToStudio
                )
                2 -> ImageAndThumbnailTab(
                    aiToolsViewModel = aiToolsViewModel,
                    studioViewModel = studioViewModel,
                    audioManager = audioManager,
                    onSendToStudio = onSendToStudio,
                    onShowMessage = onShowMessage
                )
                3 -> AudioAndTtsTab(
                    language = language,
                    audioManager = audioManager,
                    onShowMessage = onShowMessage
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PromptAndScriptTab(
    language: PromptLanguage,
    aiToolsViewModel: AiToolsViewModel,
    studioViewModel: StudioViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onSendToStudio: () -> Unit
) {
    var concept by remember { mutableStateOf("") }
    var sceneCount by remember { mutableIntStateOf(3) }
    val promptState by aiToolsViewModel.promptResultState.collectAsState()
    val scriptState by aiToolsViewModel.scriptState.collectAsState()
    val isSpeaking by audioManager.isSpeaking.collectAsState()

    GlassCard(accentHighlight = true) {
        Text(
            text = "Prompt & Scene-by-Scene Script Director",
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = concept,
            onValueChange = { concept = it },
            label = { Text("Video concept or rough idea (${language.label})") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_tools_concept_input"),
            colors = studioTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 4, 5).forEach { count ->
                StudioOptionChip(
                    label = "$count Scenes",
                    selected = sceneCount == count,
                    onClick = { sceneCount = count },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    aiToolsViewModel.generateOrImprovePrompt(
                        input = concept,
                        language = language,
                        style = "Cinematic",
                        characterSeed = "",
                        mode = "Text to Video",
                        isImprove = concept.isNotBlank()
                    )
                },
                enabled = promptState !is UiState.Loading,
                border = BorderStroke(1.dp, StudioPurpleGlow),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = StudioPurpleGlow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Craft Prompt", color = StudioPurpleGlow)
            }

            GradientStudioButton(
                text = "Generate Script",
                icon = Icons.Rounded.VideoLibrary,
                isLoading = scriptState is UiState.Loading,
                onClick = {
                    aiToolsViewModel.generateSceneScript(
                        concept = concept,
                        language = language,
                        sceneCount = sceneCount,
                        characterSeed = ""
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("generate_script_button")
            )
        }
    }

    when (val pState = promptState) {
        is UiState.Success -> {
            GlassCard {
                Text("Crafted Prompt", style = MaterialTheme.typography.titleMedium, color = StudioYellow)
                Spacer(Modifier.height(6.dp))
                Text(pState.data, style = MaterialTheme.typography.bodyLarge, color = StudioTextPrimary)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        studioViewModel.updateForm { it.copy(prompt = pState.data, language = language) }
                        onSendToStudio()
                    },
                    border = BorderStroke(1.dp, StudioYellow)
                ) {
                    Icon(Icons.Rounded.MovieCreation, contentDescription = null, tint = StudioYellow)
                    Spacer(Modifier.width(6.dp))
                    Text("Use in Video Studio", color = StudioYellow)
                }
            }
        }
        is UiState.Error -> {
            GlassCard { Text(pState.message, color = StudioError) }
        }
        else -> Unit
    }

    when (val sState = scriptState) {
        is UiState.Success -> {
            sState.data.forEach { scene ->
                SceneCard(
                    scene = scene,
                    language = language,
                    isSpeaking = isSpeaking,
                    audioManager = audioManager,
                    onAnimateScene = {
                        studioViewModel.updateForm {
                            it.copy(
                                prompt = "${scene.visualPrompt}. Camera: ${scene.cameraMove}",
                                durationSeconds = if (scene.durationSeconds in listOf(5, 10, 15)) scene.durationSeconds else 5,
                                language = language
                            )
                        }
                        onSendToStudio()
                    }
                )
            }
        }
        is UiState.Error -> {
            GlassCard { Text(sState.message, color = StudioError) }
        }
        else -> Unit
    }
}

@Composable
private fun CartoonStoryTab(
    language: PromptLanguage,
    aiToolsViewModel: AiToolsViewModel,
    studioViewModel: StudioViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onSendToStudio: () -> Unit
) {
    var premise by remember { mutableStateOf("") }
    val cartoonState by aiToolsViewModel.cartoonState.collectAsState()
    val isSpeaking by audioManager.isSpeaking.collectAsState()

    GlassCard(accentHighlight = true) {
        Text(
            text = "AI Cartoon Story Generator",
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Create multi-scene animated episodes with recurring character guides and bilingual dialogue.",
            style = MaterialTheme.typography.bodyMedium,
            color = StudioTextSecondary
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = premise,
            onValueChange = { premise = it },
            label = { Text("Cartoon story premise (${language.label})") },
            placeholder = { Text("e.g., A brave little robot and a street cat explore space temple") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cartoon_premise_input"),
            colors = studioTextFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        GradientStudioButton(
            text = "Generate Cartoon Episode",
            icon = Icons.Rounded.Brush,
            isLoading = cartoonState is UiState.Loading,
            onClick = { aiToolsViewModel.generateCartoonEpisode(premise, language) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_cartoon_button")
        )
    }

    when (val state = cartoonState) {
        is UiState.Success -> {
            val episode: CartoonEpisode = state.data
            GlassCard {
                Text(episode.title, style = MaterialTheme.typography.titleLarge, color = StudioYellow)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Character Guide: ${episode.characterGuide}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )
            }
            episode.scenes.forEach { scene ->
                SceneCard(
                    scene = scene,
                    language = language,
                    isSpeaking = isSpeaking,
                    audioManager = audioManager,
                    onAnimateScene = {
                        studioViewModel.updateForm {
                            it.copy(
                                selectedMode = VideoGenMode.CARTOON_GENERATOR,
                                prompt = "${episode.characterGuide}. ${scene.visualPrompt}",
                                language = language
                            )
                        }
                        onSendToStudio()
                    }
                )
            }
        }
        is UiState.Error -> {
            GlassCard { Text(state.message, color = StudioError) }
        }
        else -> Unit
    }
}

@Composable
private fun ImageAndThumbnailTab(
    aiToolsViewModel: AiToolsViewModel,
    studioViewModel: StudioViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onSendToStudio: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isThumbnail by remember { mutableStateOf(true) }
    var ratio by remember { mutableStateOf(AspectRatioOption.LANDSCAPE) }
    val imageState by aiToolsViewModel.imageGenState.collectAsState()

    GlassCard(accentHighlight = true) {
        Text(
            text = "AI Image & Thumbnail Generator",
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudioOptionChip(
                label = "AI Thumbnail",
                selected = isThumbnail,
                onClick = { isThumbnail = true },
                modifier = Modifier.weight(1f)
            )
            StudioOptionChip(
                label = "AI Image",
                selected = !isThumbnail,
                onClick = { isThumbnail = false },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectRatioOption.entries.forEach { opt ->
                StudioOptionChip(
                    label = opt.label,
                    selected = ratio == opt,
                    onClick = { ratio = opt },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text(if (isThumbnail) "Thumbnail scene & focal subject" else "Image prompt") },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_image_prompt_input"),
            colors = studioTextFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        GradientStudioButton(
            text = if (isThumbnail) "Generate AI Thumbnail" else "Generate AI Image",
            icon = Icons.Rounded.Image,
            isLoading = imageState is UiState.Loading,
            onClick = { aiToolsViewModel.generateImageOrThumbnail(prompt, ratio, isThumbnail) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_ai_image_button")
        )
    }

    when (val state = imageState) {
        is UiState.Loading -> {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = StudioYellow, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Synthesizing studio artwork (may take 20-45s)...", color = StudioTextSecondary)
                }
            }
        }
        is UiState.Success -> {
            val bitmap = state.data
            GlassCard(accentHighlight = true) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated Studio Artwork",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val res = audioManager.saveBitmapToGallery(bitmap, "Vasan AI Studio Art")
                            onShowMessage(
                                if (res.isSuccess) "Saved to Pictures/VasanAIStudio!"
                                else res.exceptionOrNull()?.localizedMessage ?: "Failed to save image."
                            )
                        },
                        border = BorderStroke(1.dp, StudioSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = StudioSuccess)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Image", color = StudioSuccess)
                    }
                    OutlinedButton(
                        onClick = {
                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
                            studioViewModel.setReferenceImageBytes(
                                bytes = baos.toByteArray(),
                                mimeType = "image/jpeg",
                                label = "AI Generated Frame"
                            )
                            onSendToStudio()
                        },
                        border = BorderStroke(1.dp, StudioYellow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.MovieCreation, contentDescription = null, tint = StudioYellow)
                        Spacer(Modifier.width(6.dp))
                        Text("Animate Frame", color = StudioYellow)
                    }
                }
            }
        }
        is UiState.Error -> {
            GlassCard { Text(state.message, color = StudioError) }
        }
        else -> Unit
    }
}

@Composable
private fun AudioAndTtsTab(
    language: PromptLanguage,
    audioManager: StudioSpeechAndAudioManager,
    onShowMessage: (String) -> Unit
) {
    var ttsText by remember { mutableStateOf("") }
    val isSpeaking by audioManager.isSpeaking.collectAsState()
    val activeMusic by audioManager.activeMusicTrack.collectAsState()
    val scope = rememberCoroutineScope()

    GlassCard(accentHighlight = true) {
        Text(
            text = "Text-to-Speech Voiceover Studio (${language.label})",
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ttsText,
            onValueChange = { ttsText = it },
            label = { Text("Enter narration script (${language.label})") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = studioTextFieldColors()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientStudioButton(
                text = if (isSpeaking) "Stop Voiceover" else "Speak Voiceover",
                icon = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                onClick = {
                    if (isSpeaking) {
                        audioManager.stopSpeaking()
                    } else {
                        val started = audioManager.speak(ttsText, language)
                        if (!started) {
                            onShowMessage("Enter narration text or wait for TTS engine initialization.")
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    GlassCard {
        Text(
            text = "Background Music Synthesizer Deck",
            style = MaterialTheme.typography.titleMedium,
            color = StudioTextPrimary
        )
        Spacer(Modifier.height(8.dp))
        AudioTrackOption.entries
            .filter { it != AudioTrackOption.NONE && it != AudioTrackOption.VOICEOVER_TTS }
            .forEach { track ->
                val playing = activeMusic == track
                OutlinedButton(
                    onClick = { audioManager.toggleMusicPreview(track, scope) },
                    border = BorderStroke(1.dp, if (playing) StudioYellow else StudioPurpleGlow.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Stop else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = if (playing) StudioYellow else StudioPurpleGlow
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (playing) "Playing: ${track.label} (Tap to Stop)" else "Preview: ${track.label}",
                        color = if (playing) StudioYellow else StudioTextPrimary
                    )
                }
            }
    }
}

@Composable
private fun SceneCard(
    scene: ScriptScene,
    language: PromptLanguage,
    isSpeaking: Boolean,
    audioManager: StudioSpeechAndAudioManager,
    onAnimateScene: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioStatusBadge(text = "Scene ${scene.sceneNumber} • ${scene.durationSeconds}s", color = StudioYellow)
            StudioStatusBadge(text = scene.cameraMove, color = StudioPurpleGlow)
        }
        Spacer(Modifier.height(8.dp))
        Text(scene.visualPrompt, style = MaterialTheme.typography.bodyLarge, color = StudioTextPrimary)
        if (scene.voiceoverLine.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Voiceover: \"${scene.voiceoverLine}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = StudioTextSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (scene.voiceoverLine.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        if (isSpeaking) audioManager.stopSpeaking()
                        else audioManager.speak(scene.voiceoverLine, language)
                    },
                    border = BorderStroke(1.dp, StudioPurpleGlow),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = StudioPurpleGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("TTS", color = StudioPurpleGlow)
                }
            }
            OutlinedButton(
                onClick = onAnimateScene,
                border = BorderStroke(1.dp, StudioYellow),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.MovieCreation, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send to Studio", color = StudioYellow)
            }
        }
    }
}
