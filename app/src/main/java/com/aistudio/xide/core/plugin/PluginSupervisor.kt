package com.aistudio.xide.core.plugin

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Provides an isolated coroutine scope for a plugin.
 * If a child coroutine fails, the supervisor job cancels children, but does not crash xIDE.
 */
class PluginSupervisor(val pluginId: String) {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        // Handle plugin failure: emit PluginFailed event, degrade health, etc.
    }
    
    val job = SupervisorJob()
    val scope = CoroutineScope(job + exceptionHandler)
    
    fun cancel() {
        job.cancel()
    }
}
