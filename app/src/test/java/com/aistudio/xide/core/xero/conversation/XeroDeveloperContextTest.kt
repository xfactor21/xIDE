package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildResult
import com.aistudio.xide.core.build.BuildState
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.vfs.VfsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class XeroDeveloperContextTest {

    @Test
    fun testCompleteContextAssemblyAndLimits() = runBlocking {
        val fakeWorkspaceManager = object : WorkspaceManager {
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
                return WorkspaceSession("session-123", "project-456", listOf("A.kt"), "A.kt")
            }
        }

        val diagnosticsEngine = DiagnosticsEngineImpl()
        diagnosticsEngine.addDiagnostic(
            BuildDiagnostic(
                category = "compile",
                severity = DiagnosticSeverity.ERROR,
                message = "Unresolved reference: Foo",
                rawOutput = null,
                compilerDiagnostic = null
            )
        )

        val fakeBuildResult = BuildResult(
            success = false,
            artifactInfo = null,
            diagnostics = diagnosticsEngine.activeDiagnostics.value,
            startTime = 0L,
            endTime = 100L,
            message = "Build failed due to unresolved reference"
        )

        val fakeBuildService = object : BuildService {
            override val buildState: StateFlow<BuildState> = MutableStateFlow(BuildState.FAILED)
            override val activeBuildResult: StateFlow<BuildResult?> = MutableStateFlow(fakeBuildResult)
            override suspend fun executeBuild(request: com.aistudio.xide.core.build.BuildRequest) = fakeBuildResult
            override fun cancelBuild() {}
        }

        val approvalManager = ActionApprovalManager()
        val eventBus = EventBusImpl()
        val fileChangeTracker = FileChangeTracker(eventBus, null, fakeBuildService, null)
        
        // Let subscription establish
        delay(150)
        
        // Populate changes exceeding limit of 10 to test bounded capacity
        for (i in 1..15) {
            eventBus.publish(VfsEvent.FileCreated(File("File_$i.kt").absolutePath))
        }
        
        // Brief delay to allow async flow collection to process all published events
        delay(300)

        val contextProvider = XeroContextProviderImpl(
            workspaceManager = fakeWorkspaceManager,
            diagnosticsEngine = diagnosticsEngine,
            buildService = fakeBuildService,
            approvalManager = approvalManager,
            fileChangeTracker = fileChangeTracker,
            projectIndexer = null,
            conversationHistory = ConversationHistory()
        )

        val context = contextProvider.gatherDeveloperContext(
            projectPath = "/dummy",
            activeFile = "My_API_KEY=AIzaSy123456789012345678901234567890123.kt",
            cursorLine = 12,
            cursorColumn = 4,
            selectedCodeRange = "val secret = \"AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q\""
        )

        // 1. Verify complete context assembly
        assertEquals("session-123", context.workspaceSession?.sessionId)
        assertEquals(12, context.cursorLine)
        assertEquals(4, context.cursorColumn)
        assertEquals(1, context.currentDiagnostics.size)
        assertEquals("Build failed due to unresolved reference", context.latestBuildResult?.message)

        // 2. Verify token/size limits (max 10 changes)
        assertTrue("File changes should be capped at 10", context.recentFileChanges.size <= 10)

        // 3. Verify secret removal
        assertFalse("Secret keys must not be present in active file name", context.activeFile!!.contains("AIzaSy"))
        assertFalse("Secret strings must not be leaked in selected range", context.selectedCodeRange!!.contains("AIzaSyA1B2"))
        assertTrue("Context should be marked as secure", context.isSecure())
    }
}
