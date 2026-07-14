package com.aistudio.xide.core.plugin

/**
 * Context boundary for Xero.
 * Xero can observe installed plugins, capabilities, and provider states,
 * but cannot execute plugin code or bypass permissions.
 */
data class XeroPluginContext(
    val installedPlugins: List<PluginDescriptor>,
    val availableCapabilities: List<String>
)
