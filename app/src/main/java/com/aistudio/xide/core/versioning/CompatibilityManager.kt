package com.aistudio.xide.core.versioning

/**
 * Manages platform, workspace, plugin, and provider versions.
 * Ensures future compatibility.
 */
interface CompatibilityManager {
    val platformVersion: String
    
    fun isPluginCompatible(pluginVersion: String, targetPlatformVersion: String): Boolean
    fun isWorkspaceCompatible(workspaceVersion: String): Boolean
    fun getSupportedProviderApiVersion(): String
}
