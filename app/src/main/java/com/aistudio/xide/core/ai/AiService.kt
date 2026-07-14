package com.aistudio.xide.core.ai

/**
 * High-level service boundary for xIDE intelligent operations.
 * Decoupled from specific AI vendors and models.
 */
interface AiService {
    /**
     * Requests code suggestions or generation based on prompts and workspace context.
     */
    suspend fun generateCode(prompt: String, context: AiContextSnapshot): AiServiceResult

    /**
     * Asks the intelligence layer to analyze workspace errors or build diagnostics.
     */
    suspend fun analyzeDiagnostics(diagnostics: List<String>, context: AiContextSnapshot): AiServiceResult

    /**
     * Provides explanation or documentation suggestions for a block of code.
     */
    suspend fun explainCode(code: String, context: AiContextSnapshot): AiServiceResult
}

/**
 * Result of high-level AI services.
 */
sealed class AiServiceResult {
    data class Success(val content: String) : AiServiceResult()
    data class Failure(val error: Throwable) : AiServiceResult()
}
