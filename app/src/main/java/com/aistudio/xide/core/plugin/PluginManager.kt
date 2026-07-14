package com.aistudio.xide.core.plugin

interface PluginManager {
    suspend fun discoverPlugins()
    suspend fun validatePlugin(pluginId: String)
    suspend fun enablePlugin(pluginId: String)
    suspend fun disablePlugin(pluginId: String)
    suspend fun removePlugin(pluginId: String)
    fun getInstalledPlugins(): List<PluginDescriptor>
}
