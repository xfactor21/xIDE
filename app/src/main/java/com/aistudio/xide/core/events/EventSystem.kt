package com.aistudio.xide.core.events

import kotlinx.coroutines.flow.Flow

/**
 * Event System Foundation.
 * Decoupled communication mechanism.
 */
interface EventBus {
    fun publish(event: PlatformEvent)
    fun <T : PlatformEvent> subscribe(eventType: Class<T>): Flow<T>
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
