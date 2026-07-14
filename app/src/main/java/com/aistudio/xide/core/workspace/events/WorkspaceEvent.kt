package com.aistudio.xide.core.workspace.events

import com.aistudio.xide.core.events.PlatformEvent
import java.util.UUID

sealed class WorkspaceEvent : PlatformEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Long = System.currentTimeMillis()

    data class ProjectCreated(val projectId: String, val path: String) : WorkspaceEvent()
    data class ProjectOpened(val projectId: String) : WorkspaceEvent()
    data class ProjectClosed(val projectId: String) : WorkspaceEvent()
    data class WorkspaceLoaded(val sessionId: String) : WorkspaceEvent()
    data class WorkspaceSaved(val sessionId: String) : WorkspaceEvent()
    data class WorkspaceRestored(val sessionId: String) : WorkspaceEvent()
    data class ProjectIndexed(val projectId: String) : WorkspaceEvent()
    data class WorkspaceRecovered(val sessionId: String) : WorkspaceEvent()
}
