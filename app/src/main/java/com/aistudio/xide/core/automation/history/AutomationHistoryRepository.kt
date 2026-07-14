package com.aistudio.xide.core.automation.history

import com.aistudio.xide.core.automation.TaskStatus
import java.util.concurrent.ConcurrentHashMap

data class AutomationHistoryRecord(
    val taskId: String,
    val commandName: String,
    val timestamp: Long,
    val userApproved: Boolean,
    val providerUsed: String,
    val resultStatus: TaskStatus,
    val failureReason: String?
)

interface AutomationHistoryRepository {
    fun recordTask(record: AutomationHistoryRecord)
    fun getHistory(): List<AutomationHistoryRecord>
    fun getHistoryForTask(taskId: String): AutomationHistoryRecord?
}

class InMemoryAutomationHistoryRepository : AutomationHistoryRepository {
    private val records = ConcurrentHashMap<String, AutomationHistoryRecord>()

    override fun recordTask(record: AutomationHistoryRecord) {
        records[record.taskId] = record
    }

    override fun getHistory(): List<AutomationHistoryRecord> {
        return records.values.sortedByDescending { it.timestamp }
    }

    override fun getHistoryForTask(taskId: String): AutomationHistoryRecord? {
        return records[taskId]
    }
}
