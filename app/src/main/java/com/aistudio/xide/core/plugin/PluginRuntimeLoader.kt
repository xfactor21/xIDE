package com.aistudio.xide.core.plugin

import com.aistudio.xide.core.provider.XideProvider
import com.aistudio.xide.core.service.ServiceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PluginRuntimeLoader(private val serviceRegistry: ServiceRegistry) : PluginLoader {
    private val activeSupervisors = ConcurrentHashMap<String, PluginSupervisor>()

    override suspend fun load(descriptor: PluginDescriptor) {
        withContext(Dispatchers.Default) {
            val supervisor = PluginSupervisor(descriptor.manifest.id)
            activeSupervisors[descriptor.manifest.id] = supervisor
            
            try {
                // Dynamic classloading and provider instantiation is planned to be fully implemented in a future phase.
                // At present, this registers loaded metadata; standard URLClassLoader logic will be bound here.
                
            } catch (e: Exception) {
                unload(descriptor.manifest.id)
                throw e
            }
        }
    }

    override suspend fun unload(pluginId: String) {
        withContext(Dispatchers.Default) {
            val supervisor = activeSupervisors.remove(pluginId)
            supervisor?.cancel()
            serviceRegistry.unregisterPlugin(pluginId)
        }
    }
}
