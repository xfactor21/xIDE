package com.aistudio.xide.core.provider

import com.aistudio.xide.core.events.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProviderHealthMonitor(
    private val eventBus: EventBus,
    private val providers: List<XideProvider>
) {
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startMonitoring(intervalMs: Long = 30000L) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                for (provider in providers) {
                    try {
                        val status = provider.healthCheck()
                        if (status == ProviderHealth.FAILING) {
                            // Degrade the provider in registry and emit event
                            // In real implementation, we'd emit ProviderFailed event
                        }
                    } catch (e: Exception) {
                        // Unhandled exception during healthcheck means FAILING
                    }
                }
                delay(intervalMs)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
    }
}
