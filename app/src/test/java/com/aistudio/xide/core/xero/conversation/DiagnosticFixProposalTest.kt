package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class DiagnosticFixProposalTest {

    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var diagnosticAnalyzer: DiagnosticAnalyzer
    private lateinit var approvalManager: ActionApprovalManager
    private lateinit var vfs: VirtualFileSystem
    private lateinit var projectIndexer: ProjectIndexerImpl
    private lateinit var fixProposalService: FixProposalService

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
        vfs.setActiveWorkspaceRoot("/dummy_root")

        diagnosticsEngine = DiagnosticsEngineImpl()
        diagnosticAnalyzer = DiagnosticAnalyzer(diagnosticsEngine)
        approvalManager = ActionApprovalManager()
        fixProposalService = FixProposalServiceImpl(vfs, approvalManager)
    }

    @Test
    fun testDiagnosticFixProposalFlow() = runBlocking {
        val targetPath = "/dummy_root/MainView.kt"
        filesMap[targetPath] = "package com.example\n\nfun main() {\n    val f = Foo()\n}"

        diagnosticsEngine.addDiagnostic(
            BuildDiagnostic(
                category = "compile",
                severity = DiagnosticSeverity.ERROR,
                message = "Unresolved reference: Foo",
                rawOutput = "MainView.kt:4:13: error: unresolved reference: Foo",
                compilerDiagnostic = com.aistudio.xide.core.diagnostics.CompilerDiagnostic(
                    code = "UNRESOLVED_REFERENCE",
                    message = "Unresolved reference: Foo",
                    severity = DiagnosticSeverity.ERROR,
                    location = com.aistudio.xide.core.diagnostics.DiagnosticLocation(
                        filePath = targetPath,
                        line = 4,
                        column = 13
                    )
                )
            )
        )

        // 1. Group / analyze the error
        val groups = diagnosticAnalyzer.analyzeDiagnostics()
        assertEquals(1, groups.size)
        val unresolvedGroup = groups.first()
        assertTrue(unresolvedGroup.primaryError.contains("Unresolved reference: Foo"))

        // 2. Generate a fix proposal
        val proposal = fixProposalService.proposeDiagnosticFix(unresolvedGroup, targetPath)
        assertNotNull(proposal)

        // 3. Verify proposal metrics and preview details
        assertEquals(1, proposal!!.affectedFiles.size)
        assertEquals(targetPath, proposal.affectedFiles.first())
        assertTrue("Lines should be added for the missing import", proposal.preview.linesAdded > 0)
        
        // 4. Verify no direct modifications occurred (unmodified virtual file content)
        val fileContent = vfs.openFile(targetPath)
        assertTrue("Original file content should be unmodified until approved", fileContent.contains("val f = Foo()"))
        assertFalse("Original file should not contain the suggested import yet", fileContent.contains("import com.example.symbols.Foo"))

        // 5. Verify the action proposal is pending approval inside the ActionApprovalManager
        val pendingActions = approvalManager.getPendingActions()
        assertEquals(1, pendingActions.size)
        val action = pendingActions.first() as com.aistudio.xide.core.ai.actions.AIAction.ModifyFile
        assertEquals(targetPath, action.path)
        assertTrue("Proposed action content should introduce the correct import statement", action.proposedContent.contains("import com.example.symbols.Foo"))
    }
}
