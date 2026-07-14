package com.aistudio.xide.core.intelligence

data class ModelRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val temperature: Float? = null,
    val responseMimeType: String? = null,
    val responseSchema: String? = null
)

sealed class ModelResponse {
    data class Success(val text: String) : ModelResponse()
    data class Failure(val error: Throwable) : ModelResponse()
}

interface LanguageModelProvider {
    suspend fun generate(request: ModelRequest): ModelResponse
}
