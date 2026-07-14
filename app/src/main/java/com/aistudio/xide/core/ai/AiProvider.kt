package com.aistudio.xide.core.ai

import com.aistudio.xide.core.provider.XideProvider

/**
 * Pluggable AI provider interface representing a decoupled, provider-independent model backend.
 */
interface AiProvider : XideProvider {
    /**
     * Executes an AI model chat or completion request against the model backend.
     */
    suspend fun execute(request: AiRequest): AiResponse
    
    /**
     * Checks if this provider can handle a specific model capability target.
     */
    fun supportsCapability(capability: String): Boolean
}
