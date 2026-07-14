package com.aistudio.xide.core.ai.actions

import com.aistudio.xide.core.ai.AiContextSnapshot
import com.aistudio.xide.core.ai.AiService
import com.aistudio.xide.core.ai.AiServiceResult
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.vfs.local.LocalFileSystemProvider
import com.aistudio.xide.core.xero.AnalysisPlan
import com.aistudio.xide.core.xero.XeroCoreImpl
import com.aistudio.xide.core.xero.XeroMemory
import com.aistudio.xide.core.xero.XeroMemoryContext
import com.aistudio.xide.core.xero.XeroAction
import com.aistudio.xide.core.xero.ExecutionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class XeroActionBoundaryTest {

    private lateinit var tempDir: File
    private lateinit var vfs: VirtualFileSystem
    private lateinit var xeroCore: XeroCoreImpl
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var fileOperationProvider: AIFileOperationProvider
    private lateinit var changeHistory: ChangeHistory

    @Before
    fun setUp() {
        tempDir = File.createTempFile("xero-boundary-test", "")
        tempDir.delete()
        tempDir.mkdirs()
        File(tempDir, "src").mkdirs()

        val fsProvider = LocalFileSystemProvider()
        val eventBus = EventBusImpl()
        val indexer = ProjectIndexerImpl()
        
        vfs = VirtualFileSystem(fsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        val diagnosticsEngine = DiagnosticsEngineImpl()
        approvalManager = ActionApprovalManager()
        fileOperationProvider = AIFileOperationProvider(vfs, diagnosticsEngine)
        changeHistory = ChangeHistory(vfs)

        val fakeAiService = object : AiService {
            override suspend fun generateCode(prompt: String, context: AiContextSnapshot): AiServiceResult =
                AiServiceResult.Success("// Success")
            override suspend fun analyzeDiagnostics(diagnostics: List<String>, context: AiContextSnapshot): AiServiceResult =
                AiServiceResult.Success("")
            override suspend fun explainCode(code: String, context: AiContextSnapshot): AiServiceResult =
                AiServiceResult.Success("")
        }

        val fakeAiContextManager = com.aistudio.xide.core.ai.AiContextManagerImpl(
            projectIndexer = indexer,
            activeProjectRootProvider = { tempDir.absolutePath },
            diagnosticsEngine = diagnosticsEngine
        )

        val memoryContext = object : XeroMemoryContext {
            override suspend fun getMemory(): XeroMemory = XeroMemory(emptyMap(), emptyList(), emptyMap(), emptyMap())
            override suspend fun recordAction(action: String) {}
            override suspend fun recordSolution(problem: String, solution: String) {}
        }

        xeroCore = XeroCoreImpl(
            aiService = fakeAiService,
            aiContextManager = fakeAiContextManager,
            projectIndexer = indexer,
            projectStructureService = null,
            memoryContext = memoryContext,
            actionPlanner = null,
            automationService = null,
            fileChangeTracker = null,
            buildService = null,
            workspaceManager = null,
            approvalManager = approvalManager,
            fileOperationProvider = fileOperationProvider,
            changeHistory = changeHistory
        )
    }

    @Test
    fun testBypassAttemptBlocked() = runBlocking {
        xeroCore.attachToProject(tempDir.absolutePath)

        val plan = xeroCore.analyze("create a file named main.kt", emptyList())

        val pending = approvalManager.getPendingActions()
        assertEquals(1, pending.size)
        val actionId = pending.first().actionId
        assertEquals(ActionApprovalState.PENDING, approvalManager.getActionState(actionId))

        val result = xeroCore.execute(plan)
        assertTrue(result is ExecutionResult.Failure)
        assertTrue((result as ExecutionResult.Failure).error is SecurityException)

        val mainFile = File(tempDir, "src/NewFile.kt")
        assertFalse(mainFile.exists())
    }

    @Test
    fun testApprovedExecutionSucceeds() = runBlocking {
        xeroCore.attachToProject(tempDir.absolutePath)

        val plan = xeroCore.analyze("create a file named main.kt", emptyList())

        val pending = approvalManager.getPendingActions()
        assertEquals(1, pending.size)
        val actionId = pending.first().actionId

        approvalManager.approveAction(actionId)
        assertEquals(ActionApprovalState.APPROVED, approvalManager.getActionState(actionId))

        val result = xeroCore.execute(plan)
        assertTrue(result is ExecutionResult.Success)

        val createdFile = File(tempDir, "src/NewFile.kt")
        assertTrue(createdFile.exists())
    }
}
