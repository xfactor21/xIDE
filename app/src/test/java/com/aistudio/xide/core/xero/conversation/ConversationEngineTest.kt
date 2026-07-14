package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildState
import com.aistudio.xide.core.build.BuildResult
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.intelligence.CodeNavigator
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConversationEngineTest {

    private lateinit var contextSelector: ContextSelector
    private lateinit var conversationEngine: XeroConversationEngineImpl
    private lateinit var approvalManager: ActionApprovalManager

    @Before
    fun setUp() {
        val fakeWorkspaceManager = object : WorkspaceManager {
            override val providerId: String = "ws"
            override val providerName: String = "ws"
            override val providerVersion: String = "1.0"
            override val supportedFeatures: List<String> = emptyList()
            override val requirements: List<String> = emptyList()
            override val limitations: List<String> = emptyList()
            override val description: String = ""
            override val author: String = ""
            override val compatibilityVersion: String = "1.0"

            override suspend fun healthCheck() = com.aistudio.xide.core.provider.ProviderHealth.HEALTHY
            override suspend fun initialize() {}
            override suspend fun shutdown() {}

            override fun createProject(name: String, path: String) {}
            override fun openProject(projectId: String) {}
            override fun closeCurrentProject() {}
            override fun getActiveWorkspace(): WorkspaceSession {
                return WorkspaceSession("session-1", "project-1", listOf("MyController.kt"), "MyController.kt")
            }
        }

        approvalManager = ActionApprovalManager()
        contextSelector = ContextSelector(fakeWorkspaceManager, null, null, null, null, null, null)
        
        conversationEngine = XeroConversationEngineImpl(
            workspaceManager = fakeWorkspaceManager,
            projectIndexer = null,
            codeNavigator = null,
            diagnosticsEngine = null,
            buildService = null,
            approvalManager = approvalManager,
            codeExplanationService = null,
            diagnosticAnalyzer = null,
            contextSelector = contextSelector
        )
    }

    @Test
    fun testExplainCodeRouting() = runBlocking {
        val response = conversationEngine.handleDeveloperRequest(
            question = "Explain this file MyController.kt",
            projectPath = "/dummy/project",
            activeFile = "MyController.kt"
        )
        assertTrue(response is XeroResponse.Explanation)
        val explanation = response as XeroResponse.Explanation
        assertTrue(explanation.summary.contains("MyController.kt"))
        assertEquals(XeroAssistantState.IDLE, conversationEngine.assistantState.value)
        assertEquals(1, conversationEngine.conversationHistory.getHistory().size)
    }

    @Test
    fun testCreateProposalRouting() = runBlocking {
        val response = conversationEngine.handleDeveloperRequest(
            question = "Please propose change to add spacing",
            projectPath = "/dummy/project",
            activeFile = "MyController.kt"
        )
        assertTrue(response is XeroResponse.ActionProposal)
        val proposal = response as XeroResponse.ActionProposal
        assertEquals(1, proposal.proposedActions.size)
        
        // Assistant transitions to WAITING_APPROVAL on a proposal
        assertEquals(XeroAssistantState.WAITING_APPROVAL, conversationEngine.assistantState.value)
        
        // ApprovalManager tracks the proposed action
        assertEquals(1, approvalManager.getPendingActions().size)
    }
}
