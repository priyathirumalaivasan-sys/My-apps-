package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AspectRatioOption
import com.example.data.model.CartoonEpisode
import com.example.data.model.PromptLanguage
import com.example.data.model.ScriptScene
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class FirebaseAiStudioRepository {

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val scriptSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "scenes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "sceneNumber" to Schema.integer(description = "1-based scene number"),
                            "durationSeconds" to Schema.integer(description = "5, 10, or 15"),
                            "visualPrompt" to Schema.string(description = "Detailed visual prompt for AI video model"),
                            "cameraMove" to Schema.string(description = "Camera movement instruction"),
                            "voiceoverLine" to Schema.string(description = "Narration line in requested language"),
                            "musicMood" to Schema.string(description = "Background music mood")
                        )
                    ),
                    description = "Ordered list of video scenes"
                )
            )
        )
    }

    private val cartoonSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Episode title"),
                "characterGuide" to Schema.string(description = "Consistent visual character design sheet"),
                "scenes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "sceneNumber" to Schema.integer(),
                            "durationSeconds" to Schema.integer(),
                            "visualPrompt" to Schema.string(),
                            "cameraMove" to Schema.string(),
                            "voiceoverLine" to Schema.string(),
                            "musicMood" to Schema.string()
                        )
                    )
                )
            )
        )
    }

    private val scriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptSchema
            }
        )
    }

    private val cartoonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = cartoonSchema
            }
        )
    }

    suspend fun generateOrImprovePrompt(
        userInput: String,
        language: PromptLanguage,
        visualStyle: String,
        characterSeed: String,
        generationMode: String,
        isImprove: Boolean
    ): Result<String> {
        val action = if (isImprove) {
            "Enhance and expand this prompt into a production-grade AI video prompt"
        } else {
            "Generate a fresh, vivid, cinematic AI video prompt based on this topic"
        }
        val langInstruction = if (language == PromptLanguage.TAMIL) {
            "The user supports Tamil and English. Provide the refined AI video prompt in rich English (for global video models) followed by a natural Tamil translation/caption on a new line."
        } else {
            "Write the final prompt in crisp, vivid English cinematography language."
        }
        val seedNote = if (characterSeed.isNotBlank()) {
            "Maintain strict character consistency with this seed: $characterSeed."
        } else ""

        val fullPrompt = """
            You are an expert AI film director for Vasan AI Studio.
            Task: $action.
            Mode: $generationMode. Visual Style: $visualStyle.
            $seedNote
            $langInstruction
            Include subject, action, lighting, lens/camera feel, atmosphere, and color palette. Keep it concise (2-4 sentences). Do not include markdown bullets or preambles.
            Input: ${userInput.ifBlank { "A dramatic futuristic cityscape at twilight with golden neon rain" }}
        """.trimIndent()

        return runAiTextCall { textModel.generateContent(fullPrompt).text.orEmpty().trim() }
    }

    suspend fun generateNegativePrompt(
        prompt: String,
        visualStyle: String
    ): Result<String> {
        val fullPrompt = """
            Generate a concise comma-separated negative prompt for an AI video generator (Kling / Veo / Runway) to avoid visual artifacts.
            Style: $visualStyle
            Scene: $prompt
            Return ONLY the comma-separated negative terms (e.g. blurry, extra limbs, watermark, flicker, overexposed, jittery motion).
        """.trimIndent()
        return runAiTextCall { textModel.generateContent(fullPrompt).text.orEmpty().trim() }
    }

    suspend fun buildCharacterConsistencySeed(description: String): Result<String> {
        val fullPrompt = """
            Create a compact, reusable AI Character Consistency Seed for multi-scene video generation.
            Include exact hair, outfit colors, facial features, age, accessories, and lighting signature so the character looks identical across shots.
            Character input: $description
            Return 1 dense paragraph under 60 words with no markdown.
        """.trimIndent()
        return runAiTextCall { textModel.generateContent(fullPrompt).text.orEmpty().trim() }
    }

    suspend fun generateSceneScript(
        concept: String,
        language: PromptLanguage,
        sceneCount: Int,
        characterSeed: String
    ): Result<List<ScriptScene>> {
        val langLabel = if (language == PromptLanguage.TAMIL) "Tamil (தமிழ்) for voiceoverLine and English for visualPrompt" else "English"
        val prompt = """
            Create a $sceneCount-scene AI video production script for: "$concept".
            Character consistency guide: ${characterSeed.ifBlank { "Use consistent recurring protagonist" }}.
            Language for voiceoverLine: $langLabel.
            Each scene durationSeconds must be 5, 10, or 15.
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                scriptModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty script JSON"))
            val arr = json.getJSONArray("scenes")
            val scenes = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                ScriptScene(
                    sceneNumber = obj.optInt("sceneNumber", idx + 1),
                    durationSeconds = obj.optInt("durationSeconds", 5),
                    visualPrompt = obj.optString("visualPrompt"),
                    cameraMove = obj.optString("cameraMove", "Slow Dolly In"),
                    voiceoverLine = obj.optString("voiceoverLine"),
                    musicMood = obj.optString("musicMood", "Cinematic Score")
                )
            }
            Result.success(scenes)
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Script generation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Script generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateCartoonStory(
        premise: String,
        language: PromptLanguage
    ): Result<CartoonEpisode> {
        val langNote = if (language == PromptLanguage.TAMIL) "Tamil (தமிழ்) for voiceoverLine dialogue, vibrant 3D/2D cartoon style English visualPrompt" else "English"
        val prompt = """
            Create a 4-scene AI Cartoon Story Episode based on: "$premise".
            Define a charming recurring characterGuide and 4 sequential animated scenes.
            Language for voiceoverLine: $langNote.
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                cartoonModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty cartoon JSON"))
            val arr = json.getJSONArray("scenes")
            val scenes = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                ScriptScene(
                    sceneNumber = obj.optInt("sceneNumber", idx + 1),
                    durationSeconds = obj.optInt("durationSeconds", 5),
                    visualPrompt = obj.optString("visualPrompt"),
                    cameraMove = obj.optString("cameraMove", "Tracking Follow Shot"),
                    voiceoverLine = obj.optString("voiceoverLine"),
                    musicMood = obj.optString("musicMood", "Playful Orchestral")
                )
            }
            Result.success(
                CartoonEpisode(
                    title = json.optString("title", "Cartoon Episode"),
                    characterGuide = json.optString("characterGuide"),
                    scenes = scenes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Cartoon story generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cartoon story generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: AspectRatioOption,
        isThumbnail: Boolean
    ): Result<Bitmap> {
        val ratio = when (aspectRatio) {
            AspectRatioOption.LANDSCAPE -> AspectRatio.LANDSCAPE_16x9
            AspectRatioOption.PORTRAIT -> AspectRatio.PORTRAIT_9x16
            AspectRatioOption.SQUARE -> AspectRatio.SQUARE_1x1
        }
        val finalPrompt = if (isThumbnail) {
            "High-impact cinematic YouTube/video thumbnail, dramatic rim lighting, bold focal composition, ultra-sharp 4K studio render: $prompt"
        } else {
            prompt
        }

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(ratio)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                imageModel.generateContent(finalPrompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Image model returned no image."))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Image generation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    private suspend fun runAiTextCall(block: suspend () -> String): Result<String> {
        return try {
            val text = withTimeout(AI_TIMEOUT_MS) { block() }
            if (text.isBlank()) {
                Result.failure(IllegalStateException("Model returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI request timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI text call failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "FirebaseAiStudioRepo"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
