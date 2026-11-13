package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.AiVideoModel
import com.example.data.model.AspectRatioOption
import com.example.data.model.AudioTrackOption
import com.example.data.model.CameraMovement
import com.example.data.model.CreditTransaction
import com.example.data.model.PromptLanguage
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoGenMode
import com.example.data.model.VideoJob
import com.example.data.model.VisualStyle
import com.example.data.remote.BackendHealthResponse
import com.example.data.remote.VasanBackendClientFactory
import com.example.data.repository.SettingsDataStore
import com.example.data.repository.StudioFirestoreRepository
import com.example.data.repository.StudioSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class BackendHealthState(
    val isChecking: Boolean = false,
    val isConfigured: Boolean = false,
    val isReachable: Boolean = false,
    val health: BackendHealthResponse? = null,
    val message: String = "Backend URL not configured yet."
)

data class GenerationFormState(
    val selectedModel: AiVideoModel = AiVideoModel.VEO,
    val selectedMode: VideoGenMode = VideoGenMode.TEXT_TO_VIDEO,
    val prompt: String = "",
    val negativePrompt: String = "blurry, watermark, distorted anatomy, flicker, low quality",
    val language: PromptLanguage = PromptLanguage.ENGLISH,
    val durationSeconds: Int = 5,
    val aspectRatio: AspectRatioOption = AspectRatioOption.LANDSCAPE,
    val resolution: String = "1080p",
    val fps: Int = 24,
    val cameraMovement: CameraMovement = CameraMovement.DOLLY_IN,
    val motionIntensity: Int = 6,
    val visualStyle: VisualStyle = VisualStyle.CINEMATIC,
    val audioTrack: AudioTrackOption = AudioTrackOption.CINEMATIC_SCORE,
    val selectedProjectId: String? = null,
    val referenceMediaUri: Uri? = null,
    val referenceMediaBytes: ByteArray? = null,
    val referenceMediaMimeType: String? = null,
    val referenceMediaLabel: String? = null
) {
    val estimatedCredits: Int
        get() {
            val base = selectedModel.baseCredits
            val durationMult = when (durationSeconds) {
                10 -> 2
                15 -> 3
                else -> 1
            }
            val resExtra = if (resolution == "1080p") 5 else 0
            val fpsExtra = if (fps == 60) 5 else 0
            return base * durationMult + resExtra + fpsExtra
        }
}

class StudioViewModel(
    private val firestoreRepo: StudioFirestoreRepository,
    private val settingsDataStore: SettingsDataStore,
    private val currentUserId: String
) : ViewModel() {

    val settings: StateFlow<StudioSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StudioSettings())

    val userProfile: StateFlow<UiState<UserProfile?>> = firestoreRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMsg(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val projects: StateFlow<UiState<List<StudioProject>>> = firestoreRepo.observeProjects(currentUserId)
        .map<List<StudioProject>, UiState<List<StudioProject>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMsg(error, "Failed to observe projects")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val videoJobs: StateFlow<UiState<List<VideoJob>>> = firestoreRepo.observeVideoJobs(currentUserId)
        .map<List<VideoJob>, UiState<List<VideoJob>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMsg(error, "Failed to observe video jobs")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val creditTransactions: StateFlow<UiState<List<CreditTransaction>>> =
        firestoreRepo.observeCreditTransactions(currentUserId)
            .map<List<CreditTransaction>, UiState<List<CreditTransaction>>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreMsg(error, "Failed to observe credit ledger")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _adminUsers = MutableStateFlow<UiState<List<UserProfile>>>(UiState.Idle)
    val adminUsers: StateFlow<UiState<List<UserProfile>>> = _adminUsers.asStateFlow()

    private val _formState = MutableStateFlow(GenerationFormState())
    val formState: StateFlow<GenerationFormState> = _formState.asStateFlow()

    private val _backendHealth = MutableStateFlow(BackendHealthState())
    val backendHealth: StateFlow<BackendHealthState> = _backendHealth.asStateFlow()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    private val _isSubmittingJob = MutableStateFlow(false)
    val isSubmittingJob: StateFlow<Boolean> = _isSubmittingJob.asStateFlow()

    init {
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile()
        }
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { s ->
                refreshBackendHealth(s.backendUrl)
            }
        }
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun updateForm(transform: (GenerationFormState) -> GenerationFormState) {
        _formState.value = transform(_formState.value)
    }

    fun setReferenceImageBytes(bytes: ByteArray, mimeType: String, label: String) {
        _formState.value = _formState.value.copy(
            referenceMediaUri = null,
            referenceMediaBytes = bytes,
            referenceMediaMimeType = mimeType,
            referenceMediaLabel = label,
            selectedMode = VideoGenMode.IMAGE_TO_VIDEO
        )
        _actionBanner.value = "Attached '$label' for Image-to-Video."
    }

    fun setReferenceMediaFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "image/jpeg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unable to read selected file.")
                if (bytes.size > 24 * 1024 * 1024) {
                    _actionBanner.value = "Selected file exceeds 24MB upload limit."
                    return@launch
                }
                _formState.value = _formState.value.copy(
                    referenceMediaUri = uri,
                    referenceMediaBytes = bytes,
                    referenceMediaMimeType = mimeType,
                    referenceMediaLabel = if (mimeType.startsWith("video")) "Reference Video" else "Reference Image"
                )
            } catch (e: Exception) {
                _actionBanner.value = "Failed to load media: ${e.localizedMessage}"
            }
        }
    }

    fun clearReferenceMedia() {
        _formState.value = _formState.value.copy(
            referenceMediaUri = null,
            referenceMediaBytes = null,
            referenceMediaMimeType = null,
            referenceMediaLabel = null
        )
    }

    fun saveBackendUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.saveBackendUrl(url)
            refreshBackendHealth(url)
            _actionBanner.value = "Backend URL saved."
        }
    }

    fun refreshBackendHealth(url: String = settings.value.backendUrl) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _backendHealth.value = BackendHealthState(
                isChecking = false,
                isConfigured = false,
                isReachable = false,
                message = "Node.js Express backend URL is not configured. Add your server URL in Settings to submit live video jobs."
            )
            return
        }
        viewModelScope.launch {
            _backendHealth.value = _backendHealth.value.copy(isChecking = true, isConfigured = true)
            try {
                val api = VasanBackendClientFactory.create(trimmed)
                val response = withContext(Dispatchers.IO) { api.getHealth() }
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _backendHealth.value = BackendHealthState(
                        isChecking = false,
                        isConfigured = true,
                        isReachable = true,
                        health = body,
                        message = "Connected to Express backend."
                    )
                } else {
                    _backendHealth.value = BackendHealthState(
                        isChecking = false,
                        isConfigured = true,
                        isReachable = false,
                        message = "Backend responded with HTTP ${response.code()}."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _backendHealth.value = BackendHealthState(
                    isChecking = false,
                    isConfigured = true,
                    isReachable = false,
                    message = "Cannot reach backend at $trimmed: ${e.localizedMessage ?: "Network error"}"
                )
            }
        }
    }

    fun submitVideoGeneration(onRequireConfig: (String) -> Unit) {
        val form = _formState.value
        if (form.prompt.isBlank()) {
            _actionBanner.value = "Please enter a video prompt or use AI Prompt Assistant first."
            return
        }
        if (form.selectedMode.requiresMedia && form.referenceMediaBytes == null) {
            _actionBanner.value = "${form.selectedMode.displayName} requires attaching a reference image or video."
            return
        }

        val backendUrl = settings.value.backendUrl.trim()
        val healthState = _backendHealth.value
        if (backendUrl.isBlank()) {
            val msg = "API Configuration Required: Node.js Express Backend URL is missing. Deploy /backend and enter its URL in Settings."
            recordConfigRequiredJob(form, msg)
            onRequireConfig(msg)
            return
        }

        val providerConfigured = healthState.health?.providers?.get(form.selectedModel.displayName)
        val missingEnv = healthState.health?.missingEnvByProvider?.get(form.selectedModel.displayName)
        if (healthState.isReachable && providerConfigured == false) {
            val keys = missingEnv?.joinToString(", ") ?: form.selectedModel.envKeys.joinToString(", ")
            val msg = "API Configuration Required: ${form.selectedModel.displayName} credentials missing on backend ($keys). Configure /backend/.env and restart server."
            recordConfigRequiredJob(form, msg)
            onRequireConfig(msg)
            return
        }

        viewModelScope.launch {
            _isSubmittingJob.value = true
            try {
                val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                    ?: throw IllegalStateException("Missing Firebase Auth ID token.")

                val cost = form.estimatedCredits.toLong()
                val jobId = firestoreRepo.createVideoJob(
                    job = VideoJob(
                        projectId = form.selectedProjectId,
                        modelProvider = form.selectedModel.displayName,
                        generationMode = form.selectedMode.displayName,
                        prompt = form.prompt,
                        negativePrompt = form.negativePrompt,
                        language = form.language.label,
                        durationSeconds = form.durationSeconds.toLong(),
                        aspectRatio = form.aspectRatio.label,
                        resolution = form.resolution,
                        fps = form.fps.toLong(),
                        cameraMovement = form.cameraMovement.label,
                        motionIntensity = form.motionIntensity.toLong(),
                        visualStyle = form.visualStyle.label,
                        audioTrack = form.audioTrack.label,
                        status = "queued",
                        progress = 5L,
                        creditsCost = cost
                    ),
                    deductCredits = true
                ).getOrThrow()

                val api = VasanBackendClientFactory.create(backendUrl)
                val textPlain = "text/plain".toMediaTypeOrNull()
                val mediaPart = form.referenceMediaBytes?.let { bytes ->
                    val mime = (form.referenceMediaMimeType ?: "image/jpeg").toMediaTypeOrNull()
                    val reqBody = bytes.toRequestBody(mime)
                    MultipartBody.Part.createFormData("referenceMedia", "reference_media", reqBody)
                }

                val response = withContext(Dispatchers.IO) {
                    api.generateVideo(
                        bearerToken = "Bearer $idToken",
                        modelProvider = form.selectedModel.displayName.toRequestBody(textPlain),
                        generationMode = form.selectedMode.displayName.toRequestBody(textPlain),
                        prompt = form.prompt.toRequestBody(textPlain),
                        negativePrompt = form.negativePrompt.toRequestBody(textPlain),
                        durationSeconds = form.durationSeconds.toString().toRequestBody(textPlain),
                        aspectRatio = form.aspectRatio.label.toRequestBody(textPlain),
                        resolution = form.resolution.toRequestBody(textPlain),
                        fps = form.fps.toString().toRequestBody(textPlain),
                        cameraMovement = form.cameraMovement.label.toRequestBody(textPlain),
                        motionIntensity = form.motionIntensity.toString().toRequestBody(textPlain),
                        visualStyle = form.visualStyle.label.toRequestBody(textPlain),
                        referenceMedia = mediaPart
                    )
                }

                if (response.isSuccessful && response.body()?.jobId != null) {
                    val remoteId = response.body()!!.jobId!!
                    firestoreRepo.updateVideoJobStatus(
                        jobId = jobId,
                        status = "processing",
                        progress = 20L,
                        remoteJobId = remoteId
                    )
                    _actionBanner.value = "Job queued on ${form.selectedModel.displayName}. Tracking progress in Gallery."
                    pollRemoteJob(backendUrl, jobId, remoteId)
                } else {
                    val errPayload = VasanBackendClientFactory.parseError(response.errorBody()?.string())
                    val isConfigErr = errPayload?.code == "API_CONFIGURATION_MISSING"
                    val missingList = errPayload?.missingKeys?.joinToString(", ")
                    val message = buildString {
                        append(errPayload?.message ?: "Backend error HTTP ${response.code()}")
                        if (!missingList.isNullOrBlank()) append(" Missing keys: $missingList")
                    }
                    firestoreRepo.updateVideoJobStatus(
                        jobId = jobId,
                        status = if (isConfigErr) "config_required" else "failed",
                        progress = 0L,
                        errorMessage = message
                    )
                    if (isConfigErr) {
                        onRequireConfig(message)
                    } else {
                        _actionBanner.value = message
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("StudioVM", "Video generation submission error", e)
                _actionBanner.value = e.localizedMessage ?: "Failed to submit video job."
            } finally {
                _isSubmittingJob.value = false
            }
        }
    }

    private fun recordConfigRequiredJob(form: GenerationFormState, message: String) {
        viewModelScope.launch {
            firestoreRepo.createVideoJob(
                job = VideoJob(
                    projectId = form.selectedProjectId,
                    modelProvider = form.selectedModel.displayName,
                    generationMode = form.selectedMode.displayName,
                    prompt = form.prompt,
                    negativePrompt = form.negativePrompt,
                    language = form.language.label,
                    durationSeconds = form.durationSeconds.toLong(),
                    aspectRatio = form.aspectRatio.label,
                    resolution = form.resolution,
                    fps = form.fps.toLong(),
                    cameraMovement = form.cameraMovement.label,
                    motionIntensity = form.motionIntensity.toLong(),
                    visualStyle = form.visualStyle.label,
                    audioTrack = form.audioTrack.label,
                    status = "config_required",
                    progress = 0L,
                    errorMessage = message,
                    creditsCost = 0L
                ),
                deductCredits = false
            )
        }
    }

    fun pollRemoteJob(backendUrl: String, firestoreJobId: String, remoteJobId: String) {
        viewModelScope.launch {
            val api = VasanBackendClientFactory.create(backendUrl)
            repeat(60) {
                delay(5000L)
                try {
                    val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                    val res = withContext(Dispatchers.IO) {
                        api.getJobStatus("Bearer $token", remoteJobId)
                    }
                    if (res.isSuccessful && res.body() != null) {
                        val body = res.body()!!
                        firestoreRepo.updateVideoJobStatus(
                            jobId = firestoreJobId,
                            status = body.status,
                            progress = body.progress.toLong(),
                            remoteJobId = remoteJobId,
                            videoUrl = body.videoUrl ?: body.downloadUrl,
                            errorMessage = body.errorMessage
                        )
                        if (body.status == "completed" || body.status == "failed") {
                            return@launch
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("StudioVM", "Status poll warning: ${e.message}")
                }
            }
        }
    }

    fun createProject(
        title: String,
        description: String,
        characterSeed: String,
        style: VisualStyle,
        ratio: AspectRatioOption
    ) {
        if (title.isBlank()) {
            _actionBanner.value = "Project title cannot be empty."
            return
        }
        viewModelScope.launch {
            val res = firestoreRepo.createProject(
                title = title,
                description = description,
                characterSeed = characterSeed,
                defaultStyle = style.label,
                defaultAspectRatio = ratio.label
            )
            _actionBanner.value = if (res.isSuccess) {
                "Project '$title' created."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to create project."
            }
        }
    }

    fun applyProjectToStudio(project: StudioProject) {
        val matchedStyle = VisualStyle.entries.firstOrNull { it.label == project.defaultStyle } ?: VisualStyle.CINEMATIC
        val matchedRatio = AspectRatioOption.entries.firstOrNull { it.label == project.defaultAspectRatio } ?: AspectRatioOption.LANDSCAPE
        _formState.value = _formState.value.copy(
            selectedProjectId = project.id,
            visualStyle = matchedStyle,
            aspectRatio = matchedRatio
        )
        _actionBanner.value = "Loaded project '${project.title}' presets into Studio."
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteProject(projectId)
        }
    }

    fun deleteVideoJob(jobId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteVideoJob(jobId)
        }
    }

    fun loadAdminUsers() {
        viewModelScope.launch {
            _adminUsers.value = UiState.Loading
            firestoreRepo.observeAllUsersForAdmin()
                .catch { error ->
                    _adminUsers.value = UiState.Error(unwrapFirestoreMsg(error, "Admin access required"))
                }
                .collect { users ->
                    _adminUsers.value = UiState.Success(users)
                }
        }
    }

    fun adminGrantCredits(targetUserId: String, newCredits: Long, plan: String, reason: String) {
        viewModelScope.launch {
            val res = firestoreRepo.adminUpdateUserCredits(targetUserId, newCredits, plan, reason)
            _actionBanner.value = if (res.isSuccess) {
                "Updated user credits to $newCredits ($plan)."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Admin update failed."
            }
        }
    }

    private fun unwrapFirestoreMsg(error: Throwable, fallback: String): String {
        val fsError = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (error is CancellationException && fsError == null) {
            throw error
        }
        return (fsError ?: error).localizedMessage ?: fallback
    }

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StudioViewModel(
                    firestoreRepo = StudioFirestoreRepository(db),
                    settingsDataStore = SettingsDataStore(app),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
