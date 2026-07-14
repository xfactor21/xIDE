package com.aistudio.xide.core.automation

import kotlinx.coroutines.flow.StateFlow

data class AutomationTask(
    val taskId: String,
    val taskType: String,
    val priority: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val error: Throwable? = null
)

enum class TaskStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
}

interface TaskQueue {
    fun enqueue(task: AutomationTask)
    fun cancel(taskId: String)
    fun observeTaskStatus(taskId: String): StateFlow<TaskStatus>
    fun observeTaskProgress(taskId: String): StateFlow<Float>
}

interface AutomationEngine {
    val taskQueue: TaskQueue
    suspend fun scheduleTask(task: AutomationTask): String
    suspend fun cancelTask(taskId: String)
    suspend fun recoverTask(taskId: String)
}
