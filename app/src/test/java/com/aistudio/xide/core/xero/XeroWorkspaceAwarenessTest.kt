package com.aistudio.xide.core.xero

import com.aistudio.xide.core.ai.AiContextManagerImpl
import com.aistudio.xide.core.ai.AiContextSnapshot
import com.aistudio.xide.core.ai.AiService
import com.aistudio.xide.core.ai.AiServiceResult
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class XeroWorkspaceAwarenessTest {

    private lateinit var tempDir: File
    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var eventBus: EventBusImpl
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var aiContextManager: AiContextManagerImpl
    private lateinit var fileChangeTracker: FileChangeTracker
    private lateinit var fakeAiService: FakeAiService
    private lateinit var fakeWorkspaceManager: FakeWorkspaceManager
    private lateinit var xeroCore: XeroCoreImpl

    @Before
    fun setUp() {
        tempDir = File.createTempFile("xero-awareness-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        indexer = ProjectIndexerImpl()
        eventBus = EventBusImpl()
        diagnosticsEngine = DiagnosticsEngineImpl()
        aiContextManager = AiContextManagerImpl(
            projectIndexer = indexer,
            activeProjectRootProvider = { tempDir.absolutePath },
            diagnosticsEngine = diagnosticsEngine
        )

        fileChangeTracker = FileChangeTracker(
            eventBus = eventBus,
            diagnosticsEngine = diagnosticsEngine,
            buildService = null,
            aiContextManager = aiContextManager
        )

        fakeAiService = FakeAiService()
        fakeWorkspaceManager = FakeWorkspaceManager(tempDir.absolutePath, "MainActivity.kt")

        val memoryContext = object : XeroMemoryContext {
            val actions = mutableListOf<String>()
            override suspend fun getMemory(): XeroMemory = XeroMemory(emptyMap(), actions, emptyMap(), emptyMap())
            override suspend fun recordAction(action: String) { actions.add(action) }
            override suspend fun recordSolution(problem: String, solution: String) {}
        }

        xeroCore = XeroCoreImpl(
            aiService = fakeAiService,
            aiContextManager = aiContextManager,
            projectIndexer = indexer,
            projectStructureService = null,
            memoryContext = memoryContext,
            actionPlanner = null,
            automationService = null,
            fileChangeTracker = fileChangeTracker,
            buildService = null,
            workspaceManager = fakeWorkspaceManager
        )
    }

    @Test
    fun testXeroGathersAccurateProjectIntelligence() = runBlocking {
        // Attach to project
        xeroCore.attachToProject(tempDir.absolutePath)

        // Inject file change to trigger recent changes
        val mainFile = File(tempDir, "MainActivity.kt")
        mainFile.writeText("class MainActivity")
        
        eventBus.publish(com.aistudio.xide.core.events.FileChanged(
            eventId = "evt-123",
            timestamp = System.currentTimeMillis(),
            filePath = mainFile.absolutePath
        ))

        // Wait for async background subscriber to collect event
        kotlinx.coroutines.delay(300)

        // Get Xero context
        val xeroContext = xeroCore.getXeroProjectContext()

        assertEquals(tempDir.absolutePath, xeroContext.rootPath)
        assertEquals(tempDir.name, xeroContext.name)
        assertEquals("MainActivity.kt", xeroContext.activeFile)
        assertTrue(xeroContext.recentChanges.any { it.contains("MainActivity.kt") })
        assertEquals("IDLE", xeroContext.buildStatus)
    }

    private class FakeAiService : AiService {
        override suspend fun generateCode(prompt: String, context: AiContextSnapshot): AiServiceResult {
            return AiServiceResult.Success("// Output")
        }

        override suspend fun analyzeDiagnostics(diagnostics: List<String>, context: AiContextSnapshot): AiServiceResult {
            return AiServiceResult.Success("// Analyzed")
        }

        override suspend fun explainCode(code: String, context: AiContextSnapshot): AiServiceResult {
            return AiServiceResult.Success("// Explained")
        }
    }

    private class FakeWorkspaceManager(
        private val rootPath: String,
        private val activeFile: String
    ) : WorkspaceManager {
        override val providerId: String = "fake.workspace"
        override val providerName: String = "Fake Workspace"
        override val providerVersion: String = "1.0.0"
        override val supportedFeatures: List<String> = emptyList()
        override val requirements: List<String> = emptyList()
        override val limitations: List<String> = emptyList()
        override val description: String = ""
        override val author: String = ""
        override val compatibilityVersion: String = "1.0.0"

        override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY
        override suspend fun initialize() {}
        override suspend fun shutdown() {}

        override fun createProject(name: String, path: String) {}
        override fun openProject(projectId: String) {}
        override fun closeCurrentProject() {}
        override fun getActiveWorkspace(): WorkspaceSession? {
            return WorkspaceSession(
                sessionId = "sess-1",
                projectId = "proj-1",
                openFiles = listOf(activeFile),
                activeFile = activeFile
            )
        }
    }
}
