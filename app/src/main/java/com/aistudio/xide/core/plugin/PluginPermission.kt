package com.aistudio.xide.core.plugin

enum class PluginPermission {
    VFS_READ,
    VFS_WRITE,
    TERMINAL_EXECUTE,
    NETWORK_ACCESS,
    BUILD_EXECUTE,
    AI_CONTEXT_ACCESS
}

interface PluginPermissionManager {
    fun hasPermission(pluginId: String, permission: PluginPermission): Boolean
    fun grantPermission(pluginId: String, permission: PluginPermission)
    fun revokePermission(pluginId: String, permission: PluginPermission)
}
