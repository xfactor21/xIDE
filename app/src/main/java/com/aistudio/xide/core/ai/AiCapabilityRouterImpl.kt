package com.aistudio.xide.core.ai

import com.aistudio.xide.core.provider.ProviderHealth
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom Exception thrown when no healthy registered AiProvider matches the capability requirements of a request.
 */
class NoSuitableAiProviderException(message: String) : Exception(message)

/**
 * Concrete implementation of the capability router.
 */
class AiCapabilityRouterImpl : AiCapabilityRouter {
    private val providers = ConcurrentHashMap<String, AiProvider>()

    override fun registerProvider(provider: AiProvider) {
        providers[provider.providerId] = provider
    }

    override fun unregisterProvider(providerId: String) {
        providers.remove(providerId)
    }

    override suspend fun route(request: AiRequest): AiProvider {
        val requiredCap = request.requiredCapability ?: "text_generation"
        
        // Find a healthy provider supporting the required capability
        val bestProvider = providers.values.firstOrNull { provider ->
            provider.healthCheck() == ProviderHealth.HEALTHY && 
            provider.supportsCapability(requiredCap)
        }
        
        return bestProvider ?: throw NoSuitableAiProviderException(
            "No healthy registered AI provider supports the required capability: '$requiredCap'. " +
            "Please register an implementation of AiProvider matching this capability target."
        )
    }

    override fun getRegisteredProviders(): List<AiProvider> {
        return providers.values.toList()
    }
}
