package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkflowStateTest {

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var buildService: BuildService
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var contextSelector: ContextSelector

    @Before
    fun setUp() {
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
                return WorkspaceSession("sess-1", "proj-1", listOf("A.kt"), "A.kt")
            }
        }

        diagnosticsEngine = DiagnosticsEngineImpl()
        buildService = object : BuildService {
            override val buildState: StateFlow<com.aistudio.xide.core.build.BuildState> = MutableStateFlow(com.aistudio.xide.core.build.BuildState.SUCCESS)
            override val activeBuildResult: StateFlow<com.aistudio.xide.core.build.BuildResult?> = MutableStateFlow(null)
            override suspend fun executeBuild(request: com.aistudio.xide.core.build.BuildRequest) = throw IllegalStateException()
            override fun cancelBuild() {}
        }

        approvalManager = ActionApprovalManager()
        contextSelector = ContextSelector(workspaceManager, null, null, diagnosticsEngine, null, buildService, null)
    }

    @Test
    fun testLifecycleTransitionsOnSuccess() = runBlocking {
        val engine = XeroConversationEngineImpl(
            workspaceManager = workspaceManager,
            projectIndexer = null,
            codeNavigator = null,
            diagnosticsEngine = diagnosticsEngine,
            buildService = buildService,
            approvalManager = approvalManager,
            codeExplanationService = null,
            diagnosticAnalyzer = null,
            contextSelector = contextSelector
        )

        assertEquals(DeveloperWorkflowState.IDLE, engine.workflowState.value)

        // Run request
        val response = engine.handleDeveloperRequest("Explain this", "/path", "A.kt")

        // Once completed, final state should be COMPLETED
        assertEquals(DeveloperWorkflowState.COMPLETED, engine.workflowState.value)
    }

    @Test
    fun testLifecycleTransitionsOnProposal() = runBlocking {
        val engine = XeroConversationEngineImpl(
            workspaceManager = workspaceManager,
            projectIndexer = null,
            codeNavigator = null,
            diagnosticsEngine = diagnosticsEngine,
            buildService = buildService,
            approvalManager = approvalManager,
            codeExplanationService = null,
            diagnosticAnalyzer = null,
            contextSelector = contextSelector
        )

        // Request action proposal
        val response = engine.handleDeveloperRequest("propose change", "/path", "A.kt")

        // Final state remains WAITING_APPROVAL
        assertEquals(DeveloperWorkflowState.WAITING_APPROVAL, engine.workflowState.value)
    }

    @Test
    fun testLifecycleTransitionsOnError() = runBlocking {
        val throwingWorkspaceManager = object : WorkspaceManager {
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
                throw RuntimeException("Simulated context selection crash")
            }
        }

        val badContextSelector = ContextSelector(throwingWorkspaceManager, null, null, diagnosticsEngine, null, buildService, null)

        val engine = XeroConversationEngineImpl(
            workspaceManager = throwingWorkspaceManager,
            projectIndexer = null,
            codeNavigator = null,
            diagnosticsEngine = diagnosticsEngine,
            buildService = buildService,
            approvalManager = approvalManager,
            codeExplanationService = null,
            diagnosticAnalyzer = null,
            contextSelector = badContextSelector
        )

        engine.handleDeveloperRequest("Explain this", "/path", null)

        assertEquals(DeveloperWorkflowState.FAILED, engine.workflowState.value)
    }
}
