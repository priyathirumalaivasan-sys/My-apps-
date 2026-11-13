package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioOption
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.VisualStyle
import com.example.ui.theme.GlassCard
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioOptionChip
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.AiToolsViewModel
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ProjectsScreen(
    studioViewModel: StudioViewModel,
    aiToolsViewModel: AiToolsViewModel,
    onOpenInStudio: () -> Unit
) {
    val projectsState by studioViewModel.projects.collectAsState()
    val seedAssistState by aiToolsViewModel.characterSeedState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "My Studio Projects",
                    subtitle = "Organize series, character consistency seeds & aspect presets"
                )
            }

            when (val state = projectsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StudioYellow)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        GlassCard {
                            Text(text = state.message, color = StudioError)
                        }
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            GlassCard(accentHighlight = true) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderSpecial,
                                    contentDescription = null,
                                    tint = StudioYellow,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "No projects created yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioTextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Create a project with a Character Consistency Seed to keep heroes, outfits, and styles locked across multiple video clips.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { showCreateDialog = true },
                                    border = BorderStroke(1.dp, StudioYellow)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = StudioYellow)
                                    Spacer(Modifier.width(6.dp))
                                    Text("New Project", color = StudioYellow)
                                }
                            }
                        }
                    } else {
                        items(state.data, key = { it.id }) { project ->
                            ProjectItemCard(
                                project = project,
                                onUseInStudio = {
                                    studioViewModel.applyProjectToStudio(project)
                                    onOpenInStudio()
                                },
                                onDelete = { studioViewModel.deleteProject(project.id) }
                            )
                        }
                    }
                }
                UiState.Idle -> Unit
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = StudioYellow,
            contentColor = StudioSurfaceElevated,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_project_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Create New Project")
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            seedAssistState = seedAssistState,
            onGenerateSeed = { desc -> aiToolsViewModel.generateCharacterSeed(desc) },
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, seed, style, ratio ->
                studioViewModel.createProject(title, desc, seed, style, ratio)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ProjectItemCard(
    project: StudioProject,
    onUseInStudio: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(accentHighlight = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = StudioTextPrimary
                )
                if (project.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextSecondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Project",
                    tint = StudioTextMuted
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudioStatusBadge(text = project.defaultStyle, color = StudioPurpleGlow)
            StudioStatusBadge(text = project.defaultAspectRatio, color = StudioYellow)
        }

        if (project.characterSeed.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Character Seed: ${project.characterSeed}",
                style = MaterialTheme.typography.bodyMedium,
                color = StudioTextSecondary
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onUseInStudio,
            border = BorderStroke(1.dp, StudioYellow.copy(alpha = 0.7f))
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Load into Studio", color = StudioYellow)
        }
    }
}

@Composable
private fun CreateProjectDialog(
    seedAssistState: UiState<String>,
    onGenerateSeed: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, VisualStyle, AspectRatioOption) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var characterSeed by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(VisualStyle.CINEMATIC) }
    var selectedRatio by remember { mutableStateOf(AspectRatioOption.LANDSCAPE) }

    LaunchedEffect(seedAssistState) {
        if (seedAssistState is UiState.Success) {
            characterSeed = seedAssistState.data
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = { Text("New Studio Project", color = StudioTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    colors = studioTextFieldColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Synopsis / Notes") },
                    maxLines = 2,
                    colors = studioTextFieldColors()
                )
                OutlinedTextField(
                    value = characterSeed,
                    onValueChange = { characterSeed = it },
                    label = { Text("Character Consistency Seed") },
                    placeholder = { Text("e.g., Arjun, 25yo, gold-trim black coat") },
                    maxLines = 3,
                    colors = studioTextFieldColors()
                )
                OutlinedButton(
                    onClick = { onGenerateSeed(characterSeed.ifBlank { description.ifBlank { title } }) },
                    enabled = seedAssistState !is UiState.Loading,
                    border = BorderStroke(1.dp, StudioPurpleGlow)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = StudioPurpleGlow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (seedAssistState is UiState.Loading) "Refining Seed..." else "AI Character Seed",
                        color = StudioPurpleGlow
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        StudioOptionChip(
                            label = ratio.label,
                            selected = selectedRatio == ratio,
                            onClick = { selectedRatio = ratio },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description, characterSeed, selectedStyle, selectedRatio) },
                enabled = title.isNotBlank()
            ) {
                Text("Create", color = StudioYellow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}
