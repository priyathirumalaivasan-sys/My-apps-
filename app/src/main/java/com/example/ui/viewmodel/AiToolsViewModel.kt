package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AspectRatioOption
import com.example.data.model.CartoonEpisode
import com.example.data.model.PromptLanguage
import com.example.data.model.ScriptScene
import com.example.data.model.UiState
import com.example.data.repository.FirebaseAiStudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiToolsViewModel(
    private val aiRepo: FirebaseAiStudioRepository = FirebaseAiStudioRepository()
) : ViewModel() {

    private val _promptResultState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val promptResultState: StateFlow<UiState<String>> = _promptResultState.asStateFlow()

    private val _negativePromptState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val negativePromptState: StateFlow<UiState<String>> = _negativePromptState.asStateFlow()

    private val _characterSeedState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val characterSeedState: StateFlow<UiState<String>> = _characterSeedState.asStateFlow()

    private val _scriptState = MutableStateFlow<UiState<List<ScriptScene>>>(UiState.Idle)
    val scriptState: StateFlow<UiState<List<ScriptScene>>> = _scriptState.asStateFlow()

    private val _cartoonState = MutableStateFlow<UiState<CartoonEpisode>>(UiState.Idle)
    val cartoonState: StateFlow<UiState<CartoonEpisode>> = _cartoonState.asStateFlow()

    private val _imageGenState = MutableStateFlow<UiState<Bitmap>>(UiState.Idle)
    val imageGenState: StateFlow<UiState<Bitmap>> = _imageGenState.asStateFlow()

    fun generateOrImprovePrompt(
        input: String,
        language: PromptLanguage,
        style: String,
        characterSeed: String,
        mode: String,
        isImprove: Boolean,
        onReady: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _promptResultState.value = UiState.Loading
            val res = aiRepo.generateOrImprovePrompt(
                userInput = input,
                language = language,
                visualStyle = style,
                characterSeed = characterSeed,
                generationMode = mode,
                isImprove = isImprove
            )
            _promptResultState.value = res.fold(
                onSuccess = {
                    onReady?.invoke(it)
                    UiState.Success(it)
                },
                onFailure = { UiState.Error(it.localizedMessage ?: "Prompt assistant failed.") }
            )
        }
    }

    fun generateNegativePrompt(prompt: String, style: String, onReady: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _negativePromptState.value = UiState.Loading
            val res = aiRepo.generateNegativePrompt(prompt, style)
            _negativePromptState.value = res.fold(
                onSuccess = {
                    onReady?.invoke(it)
                    UiState.Success(it)
                },
                onFailure = { UiState.Error(it.localizedMessage ?: "Negative prompt failed.") }
            )
        }
    }

    fun generateCharacterSeed(description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            _characterSeedState.value = UiState.Loading
            val res = aiRepo.buildCharacterConsistencySeed(description)
            _characterSeedState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Character seed failed.") }
            )
        }
    }

    fun generateSceneScript(
        concept: String,
        language: PromptLanguage,
        sceneCount: Int,
        characterSeed: String
    ) {
        if (concept.isBlank()) {
            _scriptState.value = UiState.Error("Enter a video concept first.")
            return
        }
        viewModelScope.launch {
            _scriptState.value = UiState.Loading
            val res = aiRepo.generateSceneScript(concept, language, sceneCount, characterSeed)
            _scriptState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Script generation failed.") }
            )
        }
    }

    fun generateCartoonEpisode(premise: String, language: PromptLanguage) {
        if (premise.isBlank()) {
            _cartoonState.value = UiState.Error("Enter a cartoon story idea first.")
            return
        }
        viewModelScope.launch {
            _cartoonState.value = UiState.Loading
            val res = aiRepo.generateCartoonStory(premise, language)
            _cartoonState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Cartoon story failed.") }
            )
        }
    }

    fun generateImageOrThumbnail(
        prompt: String,
        aspectRatio: AspectRatioOption,
        isThumbnail: Boolean
    ) {
        if (prompt.isBlank()) {
            _imageGenState.value = UiState.Error("Enter a visual prompt first.")
            return
        }
        viewModelScope.launch {
            _imageGenState.value = UiState.Loading
            val res = aiRepo.generateImage(prompt, aspectRatio, isThumbnail)
            _imageGenState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Image generation failed.") }
            )
        }
    }
}
