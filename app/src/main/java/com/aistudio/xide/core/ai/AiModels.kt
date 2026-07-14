package com.aistudio.xide.core.ai

/**
 * Role of the participant in the AI conversation.
 */
enum class AiRole {
    SYSTEM, USER, ASSISTANT
}

/**
 * Message in a multi-turn conversation.
 */
data class AiChatMessage(
    val role: AiRole,
    val content: String
)

/**
 * Configuration options for the AI generation task.
 */
data class AiModelConfig(
    val temperature: Float = 0.2f,
    val maxTokens: Int? = null,
    val topP: Float? = null,
    val responseMimeType: String? = null,
    val responseSchema: String? = null,
    val additionalParameters: Map<String, Any> = emptyMap()
)

/**
 * Immutable request model for AI generation.
 */
data class AiRequest(
    val messages: List<AiChatMessage>,
    val systemInstruction: String? = null,
    val config: AiModelConfig = AiModelConfig(),
    val requiredCapability: String? = null
)

/**
 * Structured response from the AI model.
 */
sealed class AiResponse {
    data class Success(
        val text: String,
        val usageMetadata: TokenUsage? = null,
        val modelName: String? = null
    ) : AiResponse()

    data class Failure(
        val error: Throwable,
        val errorMessage: String
    ) : AiResponse()
}

/**
 * Metadata representing token or resource consumption.
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
