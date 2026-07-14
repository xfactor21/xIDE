package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.intelligence.CodeExplanationService
import com.aistudio.xide.core.intelligence.CodeIntelligenceEngineImpl
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ConversationIntegrationTest {

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var diagnosticAnalyzer: DiagnosticAnalyzer
    private lateinit var buildService: BuildService
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var vfs: VirtualFileSystem
    private lateinit var projectIndexer: ProjectIndexerImpl
    private lateinit var codeIntelligence: CodeIntelligenceEngineImpl
    private lateinit var codeExplanation: CodeExplanationService
    private lateinit var contextSelector: ContextSelector
    private lateinit var diagnosticConversationService: DiagnosticConversationService
    private lateinit var fixProposalService: FixProposalService

    private val filesMap = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        filesMap.clear()
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "xide_test_conv_${System.currentTimeMillis()}")
        tempRoot.mkdirs()

        val fsProvider = object : FileSystemProvider {
            override val protocol: String = "local"
            override val providerId = "fs"
            override val providerName = "fs"
            override val providerVersion = "1.0"
            override val supportedFeatures = emptyList<String>()
            override val limitations = emptyList<String>()
            override val requirements = emptyList<String>()
            override val description = ""
            override val author = ""
            override val compatibilityVersion = "1.0"
            override suspend fun initialize() {}
            override suspend fun shutdown() {}
            override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY

            override suspend fun list(path: String) = emptyList<com.aistudio.xide.core.provider.FileNode>()
            override suspend fun exists(path: String): Boolean = filesMap.containsKey(path)
            override suspend fun delete(path: String): Boolean {
                filesMap.remove(path)
                return true
            }
            override suspend fun mkdir(path: String): Boolean = true
            override suspend fun readFile(path: String): InputStream {
                val content = filesMap[path] ?: ""
                return ByteArrayInputStream(content.toByteArray())
            }
            override suspend fun writeFile(path: String): OutputStream {
                return object : ByteArrayOutputStream() {
                    override fun close() {
                        filesMap[path] = toString()
                    }
                }
            }
        }

        projectIndexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fsProvider, EventBusImpl(), projectIndexer)
        vfs.setActiveWorkspaceRoot(tempRoot.absolutePath)

        workspaceManager = object : WorkspaceManager {
            override val providerId = "ws"
            override val providerName = "ws"
            override val providerVersion = "1.0"
            override val supportedFeatures = emptyList<String>()
            override val limitations = emptyList<String>()
            override val requirements = emptyList<String>()
            override val description = ""
            override val author = ""
            override val compatibilityVersion = "1.0"
            override suspend fun healthCheck() = com.aistudio.xide.core.provider.ProviderHealth.HEALTHY
            override suspend fun initialize() {}
            override suspend fun shutdown() {}
            override fun createProject(name: String, path: String) {}
            override fun openProject(projectId: String) {}
            override fun closeCurrentProject() {}
            override fun getActiveWorkspace(): WorkspaceSession {
                val path = File(tempRoot, "A.kt").absolutePath
                filesMap[path] = "package com.example\nclass A"
                return WorkspaceSession("sess-1", "proj-1", listOf(path), path)
            }
        }

        diagnosticsEngine = DiagnosticsEngineImpl()
        diagnosticAnalyzer = DiagnosticAnalyzer(diagnosticsEngine)
        
        buildService = object : BuildService {
            override val buildState: StateFlow<com.aistudio.xide.core.build.BuildState> = MutableStateFlow(com.aistudio.xide.core.build.BuildState.SUCCESS)
            override val activeBuildResult: StateFlow<com.aistudio.xide.core.build.BuildResult?> = MutableStateFlow(null)
            override suspend fun executeBuild(request: com.aistudio.xide.core.build.BuildRequest) = throw IllegalStateException()
            override fun cancelBuild() {}
        }

        approvalManager = ActionApprovalManager()
        codeIntelligence = CodeIntelligenceEngineImpl(projectIndexer, vfs, diagnosticsEngine, null)
        codeExplanation = CodeExplanationService(vfs, codeIntelligence)
        contextSelector = ContextSelector(workspaceManager, projectIndexer, null, diagnosticsEngine, null, buildService, vfs)
        diagnosticConversationService = DiagnosticConversationServiceImpl(diagnosticsEngine, diagnosticAnalyzer)
        fixProposalService = FixProposalServiceImpl(vfs, approvalManager)
    }

    @Test
    fun testExplainCodeIntentRouting() = runBlocking {
        val engine = XeroConversationEngineImpl(
            workspaceManager = workspaceManager,
            projectIndexer = projectIndexer,
            codeNavigator = null,
            diagnosticsEngine = diagnosticsEngine,
            buildService = buildService,
            approvalManager = approvalManager,
            codeExplanationService = codeExplanation,
            diagnosticAnalyzer = diagnosticAnalyzer,
            contextSelector = contextSelector,
            diagnosticConversationService = diagnosticConversationService,
            fixProposalService = fixProposalService,
            codeIntelligenceEngine = codeIntelligence
        )

        val activeFile = workspaceManager.getActiveWorkspace()?.activeFile
        val response = engine.handleDeveloperRequest(
            question = "Explain this class definition",
            projectPath = "/dummy",
            activeFile = activeFile
        )

        assertTrue(response is XeroResponse.Explanation)
        val explanation = response as XeroResponse.Explanation
        assertTrue(explanation.summary.contains("A.kt"))
        assertEquals(0.9, explanation.confidence, 0.01)
    }

    @Test
    fun testProposeChangeIntentRouting() = runBlocking {
        val engine = XeroConversationEngineImpl(
            workspaceManager = workspaceManager,
            projectIndexer = projectIndexer,
            codeNavigator = null,
            diagnosticsEngine = diagnosticsEngine,
            buildService = buildService,
            approvalManager = approvalManager,
            codeExplanationService = codeExplanation,
            diagnosticAnalyzer = diagnosticAnalyzer,
            contextSelector = contextSelector,
            diagnosticConversationService = diagnosticConversationService,
            fixProposalService = fixProposalService,
            codeIntelligenceEngine = codeIntelligence
        )

        val activeFile = workspaceManager.getActiveWorkspace()?.activeFile
        val response = engine.handleDeveloperRequest(
            question = "propose a spacing optimization change",
            projectPath = "/dummy",
            activeFile = activeFile
        )

        assertTrue(response is XeroResponse.ActionProposal)
        val proposalResponse = response as XeroResponse.ActionProposal
        assertEquals(1, proposalResponse.proposedActions.size)
        
        // Assert registering in approvalManager
        assertEquals(1, approvalManager.getPendingActions().size)
    }
}
