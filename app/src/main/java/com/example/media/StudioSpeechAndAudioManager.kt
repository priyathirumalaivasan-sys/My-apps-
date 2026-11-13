package com.example.media

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AudioTrackOption
import com.example.data.model.PromptLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

class StudioSpeechAndAudioManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeMusicTrack = MutableStateFlow<AudioTrackOption?>(null)
    val activeMusicTrack: StateFlow<AudioTrackOption?> = _activeMusicTrack.asStateFlow()

    private var musicJob: Job? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, language: PromptLanguage): Boolean {
        val engine = tts ?: return false
        if (!ttsReady || text.isBlank()) return false
        val locale = if (language == PromptLanguage.TAMIL) {
            Locale.forLanguageTag("ta-IN")
        } else {
            Locale.US
        }
        val res = engine.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vasan_tts_${System.currentTimeMillis()}")
        return true
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun toggleMusicPreview(track: AudioTrackOption, scope: CoroutineScope) {
        if (_activeMusicTrack.value == track || track == AudioTrackOption.NONE) {
            stopMusicPreview()
            return
        }
        stopMusicPreview()
        _activeMusicTrack.value = track
        musicJob = scope.launch(Dispatchers.Default) {
            playAmbientSynthLoop(track)
        }
    }

    fun stopMusicPreview() {
        musicJob?.cancel()
        musicJob = null
        _activeMusicTrack.value = null
    }

    private fun CoroutineScope.playAmbientSynthLoop(track: AudioTrackOption) {
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val freqs = when (track) {
            AudioTrackOption.CINEMATIC_SCORE -> doubleArrayOf(110.0, 164.81, 220.0, 261.63)
            AudioTrackOption.AMBIENT_SYNTH -> doubleArrayOf(98.0, 146.83, 196.0, 293.66)
            AudioTrackOption.TAMIL_FOLK_FUSION -> doubleArrayOf(130.81, 164.81, 196.0, 246.94)
            AudioTrackOption.UPBEAT_LOFI -> doubleArrayOf(146.83, 174.61, 220.0, 261.63)
            else -> doubleArrayOf(130.81, 196.0)
        }

        try {
            audioTrack.play()
            val chunk = ShortArray(1024)
            var sampleIndex = 0L
            while (isActive) {
                for (i in chunk.indices) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val chordMix = freqs.sumOf { f -> sin(2.0 * PI * f * t) } / freqs.size
                    val lfo = 0.55 + 0.45 * sin(2.0 * PI * 0.35 * t)
                    chunk[i] = (chordMix * lfo * 0.22 * Short.MAX_VALUE).toInt().toShort()
                    sampleIndex++
                }
                audioTrack.write(chunk, 0, chunk.size)
            }
        } catch (e: Exception) {
            Log.w("StudioAudio", "Audio preview stopped: ${e.message}")
        } finally {
            runCatching {
                audioTrack.stop()
                audioTrack.release()
            }
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap, title: String): Result<Uri> {
        return try {
            val filename = "vasan_ai_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.TITLE, title)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/VasanAIStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("Failed to create MediaStore record"))

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveBitmapToTempFile(bitmap: Bitmap): File {
        val file = File(appContext.cacheDir, "vasan_ref_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }

    fun enqueueMp4Download(url: String, title: String): Result<Long> {
        return try {
            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading Vasan AI Studio MP4")
                .setMimeType("video/mp4")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "VasanStudio_${System.currentTimeMillis()}.mp4"
                )
            val id = dm.enqueue(request)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun release() {
        stopSpeaking()
        stopMusicPreview()
        tts?.shutdown()
        tts = null
    }
}
