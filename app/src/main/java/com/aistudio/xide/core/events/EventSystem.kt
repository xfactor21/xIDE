package com.aistudio.xide.core.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Event System Foundation.
 * Decoupled communication mechanism.
 */
interface EventBus {
    fun publish(event: PlatformEvent)
    fun <T : PlatformEvent> subscribe(eventType: Class<T>): Flow<T>
}

class EventBusImpl : EventBus {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<PlatformEvent>(extraBufferCapacity = 128)

    override fun publish(event: PlatformEvent) {
        _events.tryEmit(event)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PlatformEvent> subscribe(eventType: Class<T>): Flow<T> {
        return _events
            .filter { eventType.isInstance(it) }
            .map { it as T }
    }
}


interface PlatformEvent {
    val eventId: String
    val timestamp: Long
}

// Example Events
data class BuildStarted(override val eventId: String, override val timestamp: Long, val projectPath: String) : PlatformEvent
data class BuildCompleted(override val eventId: String, override val timestamp: Long, val success: Boolean) : PlatformEvent
data class FileChanged(override val eventId: String, override val timestamp: Long, val filePath: String) : PlatformEvent
data class ProjectIndexed(override val eventId: String, override val timestamp: Long, val projectPath: String) : PlatformEvent
data class DiagnosticDetected(override val eventId: String, override val timestamp: Long, val issueId: String) : PlatformEvent

interface EventListener<T : PlatformEvent> {
    suspend fun onEvent(event: T)
}
