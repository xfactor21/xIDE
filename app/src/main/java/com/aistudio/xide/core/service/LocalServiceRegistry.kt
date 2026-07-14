package com.aistudio.xide.core.service

import com.aistudio.xide.core.provider.ProviderHealth
import java.util.concurrent.ConcurrentHashMap

class LocalServiceRegistry : ServiceRegistry {
    // Map of Service Type -> List of ServiceDescriptors
    private val services = ConcurrentHashMap<Class<*>, MutableList<ServiceDescriptor>>()
    private val implementations = ConcurrentHashMap<String, Any>() // pluginId_providerType -> instance
    private val grantedPermissions = ConcurrentHashMap.newKeySet<String>()

    override fun <T> register(
        serviceType: Class<T>,
        implementation: T,
        pluginId: String,
        capabilities: List<String>,
        permissions: List<String>
    ) {
        register(serviceType, implementation, pluginId, capabilities, permissions, com.aistudio.xide.core.provider.ProviderHealth.HEALTHY)
    }

    fun <T> register(
        serviceType: Class<T>,
        implementation: T,
        pluginId: String,
        capabilities: List<String>,
        permissions: List<String>,
        health: com.aistudio.xide.core.provider.ProviderHealth
    ) {
        val list = services.getOrPut(serviceType) { mutableListOf() }
        
        val descriptor = ServiceDescriptor(
            providerType = serviceType.name,
            pluginId = pluginId,
            priority = 10,
            health = health,
            capabilities = capabilities,
            permissions = permissions
        )
        
        list.add(descriptor)
        list.sortByDescending { it.priority }
        
        implementations["${pluginId}_${serviceType.name}"] = implementation as Any
        permissions.forEach { grantedPermissions.add(it) }
    }

    override fun <T> resolve(serviceType: Class<T>): T? {
        val list = services[serviceType] ?: return null
        val bestDescriptor = list.firstOrNull { it.health != ProviderHealth.FAILING } ?: return null
        
        val key = "${bestDescriptor.pluginId}_${serviceType.name}"
        @Suppress("UNCHECKED_CAST")
        return implementations[key] as? T
    }

    override fun <T> resolveByCapability(serviceType: Class<T>, capability: String): T? {
        val list = services[serviceType] ?: return null
        val bestDescriptor = list.firstOrNull { 
            it.health != ProviderHealth.FAILING && it.capabilities.contains(capability) 
        } ?: return null
        
        val key = "${bestDescriptor.pluginId}_${serviceType.name}"
        @Suppress("UNCHECKED_CAST")
        return implementations[key] as? T
    }

    override fun unregisterPlugin(pluginId: String) {
        services.values.forEach { list ->
            list.removeAll { it.pluginId == pluginId }
        }
        val keysToRemove = implementations.keys.filter { it.startsWith("${pluginId}_") }
        keysToRemove.forEach { implementations.remove(it) }
    }

    override fun getAllCapabilities(): Set<String> {
        return services.values.flatMap { list ->
            list.flatMap { it.capabilities }
        }.toSet()
    }

    override fun getActiveCapabilities(): Set<String> {
        return services.values.flatMap { list ->
            list.filter { it.health != com.aistudio.xide.core.provider.ProviderHealth.FAILING }.flatMap { it.capabilities }
        }.toSet()
    }

    override fun getPermissionsForCapability(capability: String): Set<String> {
        return services.values.flatMap { list ->
            list.filter { it.capabilities.contains(capability) }.flatMap { it.permissions }
        }.toSet()
    }

    override fun getAllDeclaredPermissions(): Set<String> {
        return grantedPermissions.toSet()
    }

    override fun grantPermission(permission: String) {
        grantedPermissions.add(permission)
    }

    fun clearPermissions() {
        grantedPermissions.clear()
    }
}
