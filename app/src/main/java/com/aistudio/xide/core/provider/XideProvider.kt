package com.aistudio.xide.core.provider

/**
 * Base interface for all providers in the xIDE platform.
 * Defines identity, capabilities, lifecycle, and metadata.
 */
interface XideProvider {
    // Identity
    val providerId: String
    val providerName: String
    val providerVersion: String

    // Capabilities
    val supportedFeatures: List<String>
    val requirements: List<String>
    val limitations: List<String>

    // Lifecycle
    suspend fun initialize()
    suspend fun shutdown()
    suspend fun healthCheck(): ProviderHealth

    // Metadata
    val description: String
    val author: String
    val compatibilityVersion: String
}

enum class ProviderHealth {
    HEALTHY, DEGRADED, FAILING
}
