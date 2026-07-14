package com.aistudio.xide.core.ai

/**
 * Router to match capability requests to the best available registered AiProvider.
 */
interface AiCapabilityRouter {
    /**
     * Registers a provider to the capability routing table.
     */
    fun registerProvider(provider: AiProvider)

    /**
     * Unregisters a provider from the routing table.
     */
    fun unregisterProvider(providerId: String)

    /**
     * Resolves and routes a request to the appropriate healthy provider.
     */
    suspend fun route(request: AiRequest): AiProvider

    /**
     * Returns all currently registered and healthy providers.
     */
    fun getRegisteredProviders(): List<AiProvider>
}
