package com.aistudio.xide.core.plugin

interface PluginLoader {
    suspend fun load(descriptor: PluginDescriptor)
    suspend fun unload(pluginId: String)
}
