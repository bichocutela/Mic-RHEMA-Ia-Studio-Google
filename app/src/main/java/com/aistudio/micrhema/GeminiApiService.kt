package com.aistudio.micrhema

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseModalities: List<String>? = null,
    val imageConfig: ImageConfig? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

suspend fun generatePlaceholderAlbumCover(prompt: String): String? = withContext(Dispatchers.IO) {
    try {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
            Log.e("GeminiAPI", "API Key is missing!")
            return@withContext null
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(
                parts = listOf(Part(text = prompt))
            )),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        val response = RetrofitClient.service.generateContent(apiKey, request)
        
        // Find the base64 image data in the response
        val inlineData = response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
        
        if (inlineData != null) {
            "data:${inlineData.mimeType};base64,${inlineData.data}"
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("GeminiAPI", "Error generating image: ${e.message}", e)
        null
    }
}
