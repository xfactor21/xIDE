package com.aistudio.xide.core.task

import com.aistudio.xide.core.automation.AutomationTask
import kotlinx.coroutines.flow.Flow

interface TaskExecutor {
    val executorId: String
    fun canExecute(task: AutomationTask): Boolean
    suspend fun execute(task: AutomationTask): TaskExecutionResult
    fun observeProgress(taskId: String): Flow<Float>
    suspend fun cancel(taskId: String)
}

data class TaskExecutionResult(
    val taskId: String,
    val success: Boolean,
    val output: String?,
    val error: Throwable?
)
