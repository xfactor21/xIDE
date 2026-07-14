package com.aistudio.xide.core.automation.history

import com.aistudio.xide.core.automation.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationHistoryRepositoryTest {
    @Test
    fun testRecordAndRetrieve() {
        val repo = InMemoryAutomationHistoryRepository()
        val record = AutomationHistoryRecord("task1", "build", 1000L, true, "gradle", TaskStatus.COMPLETED, null)
        
        repo.recordTask(record)
        
        val retrieved = repo.getHistoryForTask("task1")
        assertEquals("task1", retrieved?.taskId)
        assertEquals(1, repo.getHistory().size)
    }
}
