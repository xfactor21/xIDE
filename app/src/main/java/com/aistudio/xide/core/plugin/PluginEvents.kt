package com.aistudio.xide.core.plugin

import com.aistudio.xide.core.events.PlatformEvent
import java.util.UUID

sealed class PluginEvent : PlatformEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Long = System.currentTimeMillis()

    data class PluginInstalled(val pluginId: String) : PluginEvent()
    data class PluginEnabled(val pluginId: String) : PluginEvent()
    data class PluginDisabled(val pluginId: String) : PluginEvent()
    data class PluginFailed(val pluginId: String, val reason: String) : PluginEvent()
    data class ProviderRegistered(val pluginId: String, val providerType: String) : PluginEvent()
    data class ProviderRemoved(val pluginId: String, val providerType: String) : PluginEvent()
}
