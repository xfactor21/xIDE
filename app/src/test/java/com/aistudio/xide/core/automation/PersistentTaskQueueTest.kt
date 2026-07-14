package com.aistudio.xide.core.automation

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistentTaskQueueTest {
    @Test
    fun testEnqueueTask() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
        val queue = PersistentTaskQueue(ApplicationProvider.getApplicationContext())
        val task = AutomationTask("task1", "build", 1)
        
        queue.enqueue(task)
        assertEquals(TaskStatus.QUEUED, queue.observeTaskStatus("task1").value)
        
        queue.cancel("task1")
        assertEquals(TaskStatus.CANCELLED, queue.observeTaskStatus("task1").value)
    }
}
