package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildState
import com.aistudio.xide.core.build.BuildResult
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.intelligence.CodeNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ContextSelectorTest {

    @Test
    fun testSelectContextLimitsAndSafety() = runBlocking {
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
                return WorkspaceSession("session-1", "project-1", listOf("App.kt", "local.properties"), "App.kt")
            }
        }

        val fakeDiagnosticsEngine = object : DiagnosticsEngine {
            private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
            override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = _activeDiagnostics.asStateFlow()

            override fun addDiagnostic(diagnostic: BuildDiagnostic) {}
            override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {}
            override fun clearDiagnostics() {}
            override fun getFormattedDiagnosticsForContext(): List<String> = emptyList()
        }

        // Create selector
        val selector = ContextSelector(
            workspaceManager = fakeWorkspaceManager,
            projectIndexer = null,
            codeNavigator = null,
            diagnosticsEngine = fakeDiagnosticsEngine,
            fileChangeTracker = null,
            buildService = null,
            vfs = null
        )

        // 1. Verify safe file snippet hides secrets for local.properties
        val contextWithSecrets = selector.selectContext(
            intent = DeveloperIntent.EXPLAIN_CODE,
            queryText = "explain local.properties",
            projectPath = "/dummy/project",
            activeFile = "local.properties"
        )
        assertNotNull(contextWithSecrets.activeFileSnippet)
        assertTrue(contextWithSecrets.activeFileSnippet!!.contains("SECURITY BLOCK"))

        // 2. Verify normal selection when active file is null
        val contextNullFile = selector.selectContext(
            intent = DeveloperIntent.EXPLAIN_CODE,
            queryText = "explain something",
            projectPath = "/dummy/project",
            activeFile = null
        )
        assertEquals("App.kt", contextNullFile.activeFile)
    }
}
