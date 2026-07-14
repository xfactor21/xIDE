package com.aistudio.xide.core.ai.actions

import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.vfs.local.LocalFileSystemProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class AIFileOperationTest {

    private lateinit var tempDir: File
    private lateinit var vfs: VirtualFileSystem
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var fileOperationProvider: AIFileOperationProvider
    private lateinit var changeHistory: ChangeHistory

    @Before
    fun setUp() {
        tempDir = File.createTempFile("ai-file-op-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        val fsProvider = LocalFileSystemProvider()
        val eventBus = EventBusImpl()
        val indexer = ProjectIndexerImpl()
        
        vfs = VirtualFileSystem(fsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        diagnosticsEngine = DiagnosticsEngineImpl()
        fileOperationProvider = AIFileOperationProvider(vfs, diagnosticsEngine)
        changeHistory = ChangeHistory(vfs)
    }

    @Test
    fun testCreateAndRollback() = runBlocking {
        val testFilePath = File(tempDir, "test_file.txt").absolutePath
        val action = AIAction.CreateFile(
            actionId = "create-1",
            timestamp = System.currentTimeMillis(),
            source = "Xero",
            path = testFilePath,
            content = "Hello VFS!"
        )

        val result = fileOperationProvider.executeAction(action)
        assertTrue(result.success)
        assertEquals(vfs.validatePath(testFilePath), result.changedFiles.first())

        val content = vfs.openFile(testFilePath)
        assertEquals("Hello VFS!", content)

        assertNotNull(result.rollbackInfo)
        changeHistory.recordChange(result.rollbackInfo!!)

        val rolledBack = changeHistory.rollbackLastChange()
        assertTrue(rolledBack)

        assertFalse(File(testFilePath).exists())
    }

    @Test
    fun testUpdateAndRollback() = runBlocking {
        val testFilePath = File(tempDir, "test_update.txt").absolutePath
        vfs.createFile(testFilePath, "original content")

        val action = AIAction.ModifyFile(
            actionId = "update-1",
            timestamp = System.currentTimeMillis(),
            source = "Xero",
            path = testFilePath,
            proposedContent = "updated content"
        )

        val result = fileOperationProvider.executeAction(action)
        assertTrue(result.success)

        assertEquals("updated content", vfs.openFile(testFilePath))

        changeHistory.recordChange(result.rollbackInfo!!)
        assertTrue(changeHistory.rollbackLastChange())

        assertEquals("original content", vfs.openFile(testFilePath))
    }

    @Test
    fun testFailedOperationOutsideWorkspace() = runBlocking {
        val invalidPath = "/etc/passwd"
        val action = AIAction.CreateFile(
            actionId = "invalid-1",
            timestamp = System.currentTimeMillis(),
            source = "Xero",
            path = invalidPath,
            content = "attacker"
        )

        val result = fileOperationProvider.executeAction(action)
        assertFalse(result.success)
        assertNull(result.rollbackInfo)
    }
}
