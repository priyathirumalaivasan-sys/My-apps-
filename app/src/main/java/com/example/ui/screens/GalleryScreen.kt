package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.VideoJob
import com.example.media.StudioSpeechAndAudioManager
import com.example.ui.theme.GlassCard
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioOptionChip
import com.example.ui.theme.StudioPurpleGlow
import com.example.ui.theme.StudioStatusBadge
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.StudioYellow
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun GalleryScreen(
    studioViewModel: StudioViewModel,
    audioManager: StudioSpeechAndAudioManager,
    onShowMessage: (String) -> Unit
) {
    val jobsState by studioViewModel.videoJobs.collectAsState()
    val settings by studioViewModel.settings.collectAsState()
    var filter by remember { mutableStateOf("all") }
    var playingJob by remember { mutableStateOf<VideoJob?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Video Gallery & History",
                    subtitle = "Track real generation progress, stream MP4s & download"
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "All",
                        "processing" to "In Progress",
                        "completed" to "Completed",
                        "config_required" to "Config / Errors"
                    ).forEach { (key, label) ->
                        StudioOptionChip(
                            label = label,
                            selected = filter == key,
                            onClick = { filter = key }
                        )
                    }
                }
            }

            when (val state = jobsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
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
                    val filtered = state.data.filter { job ->
                        when (filter) {
                            "processing" -> job.status in listOf("queued", "processing")
                            "completed" -> job.status == "completed"
                            "config_required" -> job.status in listOf("config_required", "failed")
                            else -> true
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            GlassCard(accentHighlight = true) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_reel),
                                    contentDescription = stringResource(R.string.cd_empty_reel),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No video renders in this view",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudioTextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Head to Studio to submit a real Kling, MiniMax, Google Veo, or Runway video generation job.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextSecondary
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { job ->
                            VideoJobCard(
                                job = job,
                                onRefresh = {
                                    val remoteId = job.remoteJobId
                                    if (!remoteId.isNullOrBlank() && settings.backendUrl.isNotBlank()) {
                                        studioViewModel.pollRemoteJob(settings.backendUrl, job.id, remoteId)
                                    }
                                },
                                onPlay = { playingJob = job },
                                onDownload = {
                                    val url = job.videoUrl
                                    if (!url.isNullOrBlank()) {
                                        val res = audioManager.enqueueMp4Download(url, "Vasan Studio ${job.modelProvider}")
                                        onShowMessage(
                                            if (res.isSuccess) "MP4 download started in Notifications."
                                            else res.exceptionOrNull()?.localizedMessage ?: "Download failed."
                                        )
                                    }
                                },
                                onDelete = { studioViewModel.deleteVideoJob(job.id) }
                            )
                        }
                    }
                }
                UiState.Idle -> Unit
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (playingJob != null && !playingJob?.videoUrl.isNullOrBlank()) {
        VideoPlayerDialog(
            job = playingJob!!,
            onDismiss = { playingJob = null },
            onDownload = {
                audioManager.enqueueMp4Download(playingJob!!.videoUrl!!, "Vasan Studio Video")
                onShowMessage("MP4 download started.")
            }
        )
    }
}

@Composable
private fun VideoJobCard(
    job: VideoJob,
    onRefresh: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (job.status) {
        "completed" -> StudioSuccess
        "processing", "queued" -> StudioYellow
        "config_required" -> StudioWarning
        else -> StudioError
    }

    GlassCard(accentHighlight = job.status == "completed") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioStatusBadge(text = job.status.uppercase(), color = badgeColor)
                StudioStatusBadge(text = job.modelProvider, color = StudioPurpleGlow)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Job", tint = StudioTextMuted)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = job.prompt,
            style = MaterialTheme.typography.bodyLarge,
            color = StudioTextPrimary,
            maxLines = 3
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "${job.generationMode} • ${job.durationSeconds}s • ${job.aspectRatio} • ${job.resolution} • ${job.fps}fps • ${job.visualStyle}",
            style = MaterialTheme.typography.labelMedium,
            color = StudioTextSecondary
        )

        if (job.status in listOf("queued", "processing")) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (job.progress.coerceIn(0L, 100L) / 100f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = StudioYellow,
                trackColor = StudioSurfaceElevated
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rendering progress: ${job.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioYellow
                )
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = StudioPurpleGlow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Check Status", color = StudioPurpleGlow)
                }
            }
        }

        if (!job.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = job.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (job.status == "config_required") StudioWarning else StudioError
            )
        }

        if (job.status == "completed" && !job.videoUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPlay,
                    border = BorderStroke(1.dp, StudioYellow),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_video_button_${job.id}")
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = StudioYellow)
                    Spacer(Modifier.width(6.dp))
                    Text("Play Video", color = StudioYellow)
                }
                OutlinedButton(
                    onClick = onDownload,
                    border = BorderStroke(1.dp, StudioSuccess),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_mp4_button_${job.id}")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = StudioSuccess)
                    Spacer(Modifier.width(6.dp))
                    Text("Download MP4", color = StudioSuccess)
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerDialog(
    job: VideoJob,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = {
            Text(
                text = "${job.modelProvider} • ${job.resolution}",
                color = StudioTextPrimary
            )
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(Uri.parse(job.videoUrl))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = job.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("Download MP4", color = StudioSuccess)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StudioYellow)
            }
        }
    )
}
