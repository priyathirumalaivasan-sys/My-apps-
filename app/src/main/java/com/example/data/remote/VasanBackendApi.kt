package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class BackendHealthResponse(
    val ok: Boolean = false,
    val service: String = "",
    val providers: Map<String, Boolean> = emptyMap(),
    val missingEnvByProvider: Map<String, List<String>> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GenerateVideoResponse(
    val jobId: String? = null,
    val remoteJobId: String? = null,
    val status: String? = null,
    val progress: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val missingKeys: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class VideoStatusResponse(
    val jobId: String = "",
    val status: String = "processing",
    val progress: Int = 0,
    val videoUrl: String? = null,
    val downloadUrl: String? = null,
    val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class BackendErrorPayload(
    val code: String? = null,
    val message: String? = null,
    val missingKeys: List<String>? = null
)

interface VasanBackendService {
    @GET("api/health")
    suspend fun getHealth(): Response<BackendHealthResponse>

    @Multipart
    @POST("api/videos/generate")
    suspend fun generateVideo(
        @Header("Authorization") bearerToken: String,
        @Part("modelProvider") modelProvider: RequestBody,
        @Part("generationMode") generationMode: RequestBody,
        @Part("prompt") prompt: RequestBody,
        @Part("negativePrompt") negativePrompt: RequestBody,
        @Part("durationSeconds") durationSeconds: RequestBody,
        @Part("aspectRatio") aspectRatio: RequestBody,
        @Part("resolution") resolution: RequestBody,
        @Part("fps") fps: RequestBody,
        @Part("cameraMovement") cameraMovement: RequestBody,
        @Part("motionIntensity") motionIntensity: RequestBody,
        @Part("visualStyle") visualStyle: RequestBody,
        @Part referenceMedia: MultipartBody.Part? = null
    ): Response<GenerateVideoResponse>

    @GET("api/videos/status/{jobId}")
    suspend fun getJobStatus(
        @Header("Authorization") bearerToken: String,
        @Path("jobId") jobId: String
    ): Response<VideoStatusResponse>
}

object VasanBackendClientFactory {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun create(baseUrl: String): VasanBackendService {
        val normalized = baseUrl.trim().let {
            if (it.endsWith("/")) it else "$it/"
        }
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VasanBackendService::class.java)
    }

    fun parseError(errorBody: String?): BackendErrorPayload? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching {
            moshi.adapter(BackendErrorPayload::class.java).fromJson(errorBody)
        }.getOrNull()
    }
}
