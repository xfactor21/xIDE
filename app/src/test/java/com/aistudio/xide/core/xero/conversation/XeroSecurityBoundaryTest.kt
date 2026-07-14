package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class XeroSecurityBoundaryTest {

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var buildService: BuildService
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var vfs: VirtualFileSystem
    private lateinit var projectIndexer: ProjectIndexerImpl
    private val filesMap = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        filesMap.clear()
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
        vfs.setActiveWorkspaceRoot("/my_secure_workspace")

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
                return WorkspaceSession("sess-1", "proj-1", listOf("/my_secure_workspace/Main.kt"), "/my_secure_workspace/Main.kt")
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
    }

    @Test
    fun testWorkspaceBoundaryPathTraversalProtection() {
        // Assert that files outside workspace root are blocked at VFS layer
        try {
            vfs.validatePath("/etc/passwd")
            fail("Should have thrown SecurityException for path-traversal")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("Access denied"))
        }
    }

    @Test
    fun testXeroHasNoDirectMutationPower() = runBlocking {
        // Prepare target workspace file
        val targetPath = "/my_secure_workspace/Main.kt"
        filesMap[targetPath] = "class Main"

        val contextSelector = ContextSelector(workspaceManager, null, null, diagnosticsEngine, null, buildService, vfs)
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

        // Request proposing a change
        val response = engine.handleDeveloperRequest("propose change", "/my_secure_workspace", targetPath)

        // Verify that the file remains unchanged (unmodified at storage layer)
        assertEquals("class Main", filesMap[targetPath])

        // Verify that mutation could ONLY happen via approval queue registration
        val pending = approvalManager.getPendingActions()
        assertEquals(1, pending.size)
        assertTrue(pending.first() is com.aistudio.xide.core.ai.actions.AIAction.ModifyFile)
    }
}
