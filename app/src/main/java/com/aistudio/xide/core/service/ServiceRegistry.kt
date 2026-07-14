package com.aistudio.xide.core.service

interface ServiceRegistry {
    fun <T> register(
        serviceType: Class<T>,
        implementation: T,
        pluginId: String,
        capabilities: List<String> = emptyList(),
        permissions: List<String> = emptyList()
    )
    fun <T> resolve(serviceType: Class<T>): T?
    fun <T> resolveByCapability(serviceType: Class<T>, capability: String): T?
    fun unregisterPlugin(pluginId: String)
    fun getAllCapabilities(): Set<String>
    fun getActiveCapabilities(): Set<String>
    fun getPermissionsForCapability(capability: String): Set<String>
    fun getAllDeclaredPermissions(): Set<String>
    fun grantPermission(permission: String)
}
