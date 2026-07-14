package com.aistudio.xide.core.intelligence

import com.aistudio.xide.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null,
    val responseSchema: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

class GeminiLanguageModelProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : LanguageModelProvider {

    private val apiService: GeminiApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    override suspend fun generate(request: ModelRequest): ModelResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "UNSPECIFIED" || apiKey.contains("placeholder", ignoreCase = true)) {
            return@withContext ModelResponse.Failure(IllegalStateException("Gemini API Key is missing or invalid."))
        }

        try {
            val geminiRequest = GeminiGenerateRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = request.prompt)))),
                systemInstruction = request.systemInstruction?.let {
                    GeminiContent(parts = listOf(GeminiPart(text = it)))
                },
                generationConfig = GeminiGenerationConfig(
                    temperature = request.temperature,
                    responseMimeType = request.responseMimeType,
                    responseSchema = request.responseSchema
                )
            )

            val response = apiService.generateContent(apiKey, geminiRequest)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                ModelResponse.Success(responseText)
            } else {
                ModelResponse.Failure(IllegalStateException("No response content from Gemini model."))
            }
        } catch (e: Exception) {
            ModelResponse.Failure(e)
        }
    }
}
