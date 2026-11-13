package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AiVideoModel(
    val displayName: String,
    val badge: String,
    val summary: String,
    val baseCredits: Int,
    val envKeys: List<String>
) {
    KLING("Kling AI", "v1.6 Pro", "Photorealistic motion & long-take physics", 15, listOf("KLING_ACCESS_KEY", "KLING_SECRET_KEY")),
    MINIMAX("MiniMax Hailuo AI", "Director-01", "Expressive characters & fluid camera control", 12, listOf("MINIMAX_API_KEY")),
    VEO("Google Veo", "Veo 2", "Native 1080p cinema lighting & realistic scenes", 20, listOf("GOOGLE_VEO_API_KEY")),
    RUNWAY("Runway AI", "Gen-4 Turbo", "Keyframe animation & image-to-video precision", 18, listOf("RUNWAY_API_KEY"))
}

enum class VideoGenMode(val displayName: String, val requiresMedia: Boolean) {
    TEXT_TO_VIDEO("Text to Video", false),
    IMAGE_TO_VIDEO("Image to Video", true),
    VIDEO_TO_VIDEO("Video to Video", true),
    AI_ANIMATION("AI Animation", false),
    CARTOON_GENERATOR("Cartoon Video Generator", false),
    CHARACTER_ANIMATION("Character Animation", false)
}

enum class VisualStyle(val label: String) {
    CINEMATIC("Cinematic"),
    REALISTIC("Realistic"),
    THREE_D("3D"),
    ANIME("Anime"),
    CARTOON("Cartoon")
}

enum class AspectRatioOption(val label: String, val ratioText: String) {
    LANDSCAPE("16:9", "16:9 Widescreen"),
    PORTRAIT("9:16", "9:16 Reels / Shorts"),
    SQUARE("1:1", "1:1 Square")
}

enum class PromptLanguage(val label: String, val localeTag: String) {
    ENGLISH("English", "en-US"),
    TAMIL("Tamil", "ta-IN")
}

enum class CameraMovement(val label: String) {
    STATIC("Static Tripod"),
    DOLLY_IN("Slow Dolly In"),
    ORBIT("360° Orbital Pan"),
    CRANE_UP("Cinema Crane Rise"),
    HANDHELD("Dynamic Handheld"),
    TRACKING("Tracking Follow Shot")
}

enum class AudioTrackOption(val label: String) {
    NONE("Silent / No Music"),
    CINEMATIC_SCORE("Epic Cinematic Score"),
    AMBIENT_SYNTH("Dark Ambient Synth"),
    TAMIL_FOLK_FUSION("Tamil Cinematic Percussions"),
    UPBEAT_LOFI("Neon Lo-Fi Pulse"),
    VOICEOVER_TTS("AI Voiceover Narration")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val credits: Long = 100L,
    val plan: String = "starter",
    val role: String = "user",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudioProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val characterSeed: String = "",
    val defaultStyle: String = VisualStyle.CINEMATIC.label,
    val defaultAspectRatio: String = AspectRatioOption.LANDSCAPE.label,
    val videoCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VideoJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String? = null,
    val modelProvider: String = AiVideoModel.VEO.displayName,
    val generationMode: String = VideoGenMode.TEXT_TO_VIDEO.displayName,
    val prompt: String = "",
    val negativePrompt: String = "",
    val language: String = PromptLanguage.ENGLISH.label,
    val durationSeconds: Long = 5L,
    val aspectRatio: String = AspectRatioOption.LANDSCAPE.label,
    val resolution: String = "1080p",
    val fps: Long = 24L,
    val cameraMovement: String = CameraMovement.DOLLY_IN.label,
    val motionIntensity: Long = 6L,
    val visualStyle: String = VisualStyle.CINEMATIC.label,
    val audioTrack: String = AudioTrackOption.CINEMATIC_SCORE.label,
    val status: String = "queued",
    val progress: Long = 0L,
    val remoteJobId: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val errorMessage: String? = null,
    val creditsCost: Long = 15L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CreditTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Long = 0L,
    val reason: String = "",
    val jobId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ScriptScene(
    val sceneNumber: Int,
    val durationSeconds: Int,
    val visualPrompt: String,
    val cameraMove: String,
    val voiceoverLine: String,
    val musicMood: String
)

data class CartoonEpisode(
    val title: String,
    val characterGuide: String,
    val scenes: List<ScriptScene>
)
