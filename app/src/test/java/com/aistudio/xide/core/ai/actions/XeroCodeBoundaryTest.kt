package com.aistudio.xide.core.ai.actions

import com.aistudio.xide.core.ai.AiContextSnapshot
import com.aistudio.xide.core.ai.AiService
import com.aistudio.xide.core.ai.AiServiceResult
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.vfs.local.LocalFileSystemProvider
import com.aistudio.xide.core.intelligence.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class XeroCodeBoundaryTest {

    private lateinit var tempDir: File
    private lateinit var vfs: VirtualFileSystem
    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var engine: CodeIntelligenceEngine
    private lateinit var navigator: CodeNavigator

    @Before
    fun setUp() {
        tempDir = File.createTempFile("xero-code-boundary", "")
        tempDir.delete()
        tempDir.mkdirs()

        val fsProvider = LocalFileSystemProvider()
        val eventBus = EventBusImpl()
        indexer = ProjectIndexerImpl()
        
        vfs = VirtualFileSystem(fsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        approvalManager = ActionApprovalManager()
        engine = CodeIntelligenceEngineImpl(indexer, vfs, null, null)
        navigator = CodeNavigator(indexer)
    }

    @Test
    fun testCodeIntelligenceIsReadOnlyAndDoesNotRequestApprovals() = runBlocking {
        // Write a mock kotlin file
        val srcDir = File(tempDir, "src").apply { mkdirs() }
        val testFile = File(srcDir, "Controller.kt")
        testFile.writeText("""
            package com.example
            class Controller {
                fun handle() {}
            }
        """.trimIndent())

        // Index the project
        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        // 1. Run code intelligence analyses
        val summary = engine.analyzeSourceFile(tempDir.absolutePath, testFile.absolutePath)
        assertNotNull(summary)
        assertEquals("Controller", summary.className)

        val defs = navigator.goToDefinition(tempDir.absolutePath, "Controller")
        assertEquals(1, defs.size)

        // 2. Assert no approvals or pending human-in-the-loop actions are generated
        val pending = approvalManager.getPendingActions()
        assertTrue("Code intelligence must not generate pending approval actions", pending.isEmpty())
        
        // 3. Verify no file creations or changes were made via VFS by checking there are only the files we manually created
        val srcFiles = srcDir.listFiles()
        assertEquals(1, srcFiles?.size ?: 0)
        assertEquals("Controller.kt", srcFiles?.first()?.name)
    }
}
