package com.aistudio.xide.core.automation

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersistentTaskQueue(private val context: Context) : TaskQueue {
    private val workManager = WorkManager.getInstance(context)
    private val taskStatusMap = ConcurrentHashMap<String, MutableStateFlow<TaskStatus>>()
    private val taskProgressMap = ConcurrentHashMap<String, MutableStateFlow<Float>>()
    private val workIdMap = ConcurrentHashMap<String, UUID>()

    override fun enqueue(task: AutomationTask) {
        val statusFlow = taskStatusMap.getOrPut(task.taskId) { MutableStateFlow(TaskStatus.QUEUED) }
        val progressFlow = taskProgressMap.getOrPut(task.taskId) { MutableStateFlow(0f) }
        
        statusFlow.value = TaskStatus.QUEUED
        
        val inputData = Data.Builder()
            .putString("taskId", task.taskId)
            .putString("taskType", task.taskType)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<AutomationWorker>()
            .setInputData(inputData)
            .addTag("xide_task_${task.taskId}")
            .build()
            
        workIdMap[task.taskId] = workRequest.id
        workManager.enqueue(workRequest)
        
        // In a real implementation, we'd observe workManager.getWorkInfoByIdLiveData
        // and update the statusFlow. For this abstraction, we just enqueue.
    }

    override fun cancel(taskId: String) {
        val workId = workIdMap[taskId]
        if (workId != null) {
            workManager.cancelWorkById(workId)
            taskStatusMap[taskId]?.value = TaskStatus.CANCELLED
        } else {
            // Fallback cancellation by tag
            workManager.cancelAllWorkByTag("xide_task_${taskId}")
            taskStatusMap[taskId]?.value = TaskStatus.CANCELLED
        }
    }

    override fun observeTaskStatus(taskId: String): StateFlow<TaskStatus> {
        return taskStatusMap.getOrPut(taskId) { MutableStateFlow(TaskStatus.QUEUED) }.asStateFlow()
    }

    override fun observeTaskProgress(taskId: String): StateFlow<Float> {
        return taskProgressMap.getOrPut(taskId) { MutableStateFlow(0f) }.asStateFlow()
    }
}

class AutomationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val taskId = inputData.getString("taskId") ?: return Result.failure()
        val taskType = inputData.getString("taskType") ?: return Result.failure()
        
        try {
            // Task execution abstraction. 
            // The real execution would resolve the AutomationCommand and run it.
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
