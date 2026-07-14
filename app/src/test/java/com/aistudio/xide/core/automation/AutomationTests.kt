package com.aistudio.xide.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.aistudio.xide.core.task.*
import com.aistudio.xide.core.execution.*
import com.aistudio.xide.core.dependency.*
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.diagnostics.DiagnosticSource

@RunWith(RobolectricTestRunner::class)
class AutomationTests {

    @Test
    fun testAutomationTask() {
        val task = AutomationTask("t1", "build", 1)
        assertEquals("t1", task.taskId)
        assertEquals("build", task.taskType)
        assertNotNull(task.createdAt)
    }

    @Test
    fun testTerminalSession() {
        val session = TerminalSession("s1", "/workspace", emptyList(), "")
        assertEquals("s1", session.sessionId)
    }

    @Test
    fun testPackageDefinition() {
        val pkg = PackageDefinition("kotlin-stdlib", "1.9.0", "maven")
        assertEquals("kotlin-stdlib", pkg.name)
    }

    @Test
    fun testAutomationEvents() {
        val ev = AutomationEvent.BuildStarted("p1", "b1")
        assertEquals("p1", ev.projectId)
    }

    @Test
    fun testAutomationCommands() {
        val cmd = AutomationCommand.BuildProjectCommand("p1", "release")
        assertEquals("Build Project", cmd.name)
        assertEquals("release", cmd.buildType)
    }
}
