package com.aistudio.xide.core.registry

import com.aistudio.xide.core.provider.XideProvider

/**
 * Core Service Registry responsible for discovering and managing providers.
 */
interface ServiceRegistry {
    suspend fun <T : XideProvider> register(serviceInterface: Class<T>, implementation: T)
    suspend fun <T : XideProvider> unregister(serviceInterface: Class<T>, providerId: String)
    fun <T : XideProvider> get(serviceInterface: Class<T>): T
    fun <T : XideProvider> getAll(serviceInterface: Class<T>): List<T>
}
