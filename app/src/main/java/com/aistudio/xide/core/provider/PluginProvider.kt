package com.aistudio.xide.core.provider

/**
 * Abstraction for Plugin Management.
 */
interface PluginProvider : XideProvider {
    suspend fun loadPlugin(pluginPath: String)
    suspend fun unloadPlugin(pluginId: String)
    fun getInstalledPlugins(): List<String>
}
