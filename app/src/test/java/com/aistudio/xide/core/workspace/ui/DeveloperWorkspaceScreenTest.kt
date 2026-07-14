package com.aistudio.xide.core.workspace.ui

import com.aistudio.xide.core.ai.actions.*
import com.aistudio.xide.core.build.*
import com.aistudio.xide.core.diagnostics.*
import com.aistudio.xide.core.editor.ActiveEditorContext
import com.aistudio.xide.core.editor.CursorPosition
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.events.EventBus
import com.aistudio.xide.core.events.PlatformEvent
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.xero.conversation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DeveloperWorkspaceScreenTest {

    @Test
    fun testWorkspaceNavigationState() {
        val navManager = WorkspaceNavigationManager()
        val state = navManager.navigationState
        
        // Initial state
        assertEquals("explorer", state.value.selectedPanel)
        assertNull(state.value.activeFile)
        assertTrue(state.value.openFiles.isEmpty())

        // Select file
        navManager.selectFile("/src/Main.kt")
        assertEquals("/src/Main.kt", state.value.activeFile)
        assertEquals(1, state.value.openFiles.size)
        assertEquals("/src/Main.kt", state.value.openFiles[0])

        // Select panel
        navManager.selectPanel("xero")
        assertEquals("xero", state.value.selectedPanel)

        // Close file
        navManager.closeFile("/src/Main.kt")
        assertNull(state.value.activeFile)
        assertTrue(state.value.openFiles.isEmpty())
    }

    @Test
    fun testProjectExplorer() = kotlinx.coroutines.runBlocking {
        val mockFsProvider = object : FileSystemProvider {
            override val protocol = "local"
            override val providerId = "mock"
            override val providerName = "Mock"
            override val providerVersion = "1.0"
            override val supportedFeatures = emptyList<String>()
            override val requirements = emptyList<String>()
            override val limitations = emptyList<String>()
            override val description = "Mock"
            override val author = "Test"
            override val compatibilityVersion = "1.0"
            override suspend fun initialize() {}
            override suspend fun shutdown() {}
            override suspend fun healthCheck() = com.aistudio.xide.core.provider.ProviderHealth.HEALTHY

            override suspend fun list(path: String): List<FileNode> {
                if (path == "/root") {
                    return listOf(
                        FileNode("/root/file1.kt", "file1.kt", false, 120L, System.currentTimeMillis()),
                        FileNode("/root/dir1", "dir1", true, 0L, System.currentTimeMillis())
                    )
                }
                return emptyList()
            }
            override suspend fun readFile(path: String): InputStream = ByteArrayInputStream("content".toByteArray())
            override suspend fun writeFile(path: String): OutputStream = ByteArrayOutputStream()
            override suspend fun delete(path: String) = true
            override suspend fun mkdir(path: String) = true
            override suspend fun exists(path: String) = true
        }

        val mockEventBus = object : EventBus {
            override fun publish(event: PlatformEvent) {}
            override fun <T : PlatformEvent> subscribe(eventType: Class<T>) = emptyFlow<T>()
        }

        val mockIndexer = ProjectIndexerImpl()
        val realVfs = VirtualFileSystem(mockFsProvider, mockEventBus, mockIndexer)
        realVfs.setActiveWorkspaceRoot("/root")

        val root = realVfs.getActiveWorkspaceRoot()
        assertEquals("/root", root)

        val files = realVfs.listFiles("/root")
        assertEquals(2, files.size)
        assertEquals("file1.kt", files[0].name)
        assertFalse(files[0].isDirectory)
        assertEquals("dir1", files[1].name)
        assertTrue(files[1].isDirectory)

        val searched = realVfs.searchFiles("file1")
        assertEquals(1, searched.size)
    }

    @Test
    fun testEditorContextTracking() {
        val activeFilePath = "/root/file1.kt"
        val activeEditorContextFlow = MutableStateFlow<ActiveEditorContext?>(null)

        // Simulate file open
        activeEditorContextFlow.value = ActiveEditorContext(
            filePath = activeFilePath,
            cursorLocation = CursorPosition(1, 1),
            selectedText = null
        )

        val initialContext = activeEditorContextFlow.value
        assertNotNull(initialContext)
        assertEquals("/root/file1.kt", initialContext?.filePath)
        assertEquals(1, initialContext?.cursorLocation?.line)
        assertEquals(1, initialContext?.cursorLocation?.column)

        // Simulate selection or cursor move
        activeEditorContextFlow.value = ActiveEditorContext(
            filePath = activeFilePath,
            cursorLocation = CursorPosition(12, 5),
            selectedText = "class Awesome"
        )

        val updatedContext = activeEditorContextFlow.value
        assertEquals(12, updatedContext?.cursorLocation?.line)
        assertEquals(5, updatedContext?.cursorLocation?.column)
        assertEquals("class Awesome", updatedContext?.selectedText)
    }

    @Test
    fun testXeroPanelIntegration() {
        val mockEngine = object : XeroConversationEngine {
            override val assistantState = MutableStateFlow(XeroAssistantState.IDLE)
            override val workflowState = MutableStateFlow(DeveloperWorkflowState.IDLE)
            override val conversationHistory = ConversationHistory()
            override suspend fun handleDeveloperRequest(question: String, projectPath: String, activeFile: String?): XeroResponse {
                return XeroResponse.Explanation(
                    summary = "Mocked Response",
                    confidence = 1.0,
                    supportingFiles = emptyList(),
                    relatedSymbols = emptyList(),
                    nextSteps = listOf("Review", "Build")
                )
            }
        }

        val approvalManager = ActionApprovalManager()
        val mockAction = AIAction.CreateFile(
            actionId = "action-1",
            timestamp = System.currentTimeMillis(),
            source = "xero",
            path = "/root/newfile.kt",
            content = "println()"
        )

        // Propose action
        val state = approvalManager.proposeAction(mockAction)
        assertEquals(ActionApprovalState.PENDING, state)

        val pending = approvalManager.getPendingActions()
        assertEquals(1, pending.size)
        assertEquals("action-1", pending[0].actionId)

        // Approve action
        approvalManager.approveAction("action-1")
        assertEquals(ActionApprovalState.APPROVED, approvalManager.getActionState("action-1"))
        assertTrue(approvalManager.getPendingActions().isEmpty())
    }

    @Test
    fun testBuildPanel() {
        val mockBuildService = object : BuildService {
            override val buildState = MutableStateFlow(BuildState.IDLE)
            override val activeBuildResult = MutableStateFlow<BuildResult?>(null)
            override suspend fun executeBuild(request: BuildRequest): BuildResult {
                buildState.value = BuildState.RUNNING
                val result = BuildResult(
                    success = true,
                    artifactInfo = ArtifactInfo("/bin/app.apk", "debug", System.currentTimeMillis()),
                    diagnostics = emptyList(),
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 1000,
                    message = "Build finished successfully"
                )
                activeBuildResult.value = result
                buildState.value = BuildState.SUCCESS
                return result
            }
            override fun cancelBuild() {
                buildState.value = BuildState.CANCELLED
            }
        }

        assertEquals(BuildState.IDLE, mockBuildService.buildState.value)
        assertNull(mockBuildService.activeBuildResult.value)
    }

    @Test
    fun testDiagnosticsPanel() {
        val mockDiagnosticsEngine = object : DiagnosticsEngine {
            val list = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
            override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = list
            override fun addDiagnostic(diagnostic: BuildDiagnostic) {}
            override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {}
            override fun clearDiagnostics() {}
            override fun getFormattedDiagnosticsForContext(): List<String> = emptyList()
        }

        assertTrue(mockDiagnosticsEngine.activeDiagnostics.value.isEmpty())
    }

    @Test
    fun testSecurityBoundaryUI() {
        val approvalManager = ActionApprovalManager()
        val mockAction = AIAction.ModifyFile(
            actionId = "secure-action",
            timestamp = System.currentTimeMillis(),
            source = "xero",
            path = "/root/secret.kt",
            proposedContent = "secret_key = REDACTED"
        )

        approvalManager.proposeAction(mockAction)
        
        // Assert action is strictly locked to PENDING and cannot be processed without explicit approval
        assertEquals(ActionApprovalState.PENDING, approvalManager.getActionState("secure-action"))
    }
}
