package com.aistudio.xide.core.automation

import com.aistudio.xide.core.events.PlatformEvent
import java.util.UUID

sealed class AutomationEvent : PlatformEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Long = System.currentTimeMillis()

    data class BuildStarted(val projectId: String, val buildId: String) : AutomationEvent()
    data class BuildCompleted(val projectId: String, val buildId: String, val success: Boolean) : AutomationEvent()
    data class BuildFailed(val projectId: String, val buildId: String, val error: String) : AutomationEvent()
    
    data class TaskQueued(val taskId: String, val taskType: String) : AutomationEvent()
    data class TaskStarted(val taskId: String) : AutomationEvent()
    data class TaskCompleted(val taskId: String, val success: Boolean) : AutomationEvent()
    data class TaskFailed(val taskId: String, val error: String) : AutomationEvent()
    
    data class DependencyInstalled(val packageName: String, val version: String) : AutomationEvent()
    data class TerminalCommandExecuted(val sessionId: String, val command: String) : AutomationEvent()
}
