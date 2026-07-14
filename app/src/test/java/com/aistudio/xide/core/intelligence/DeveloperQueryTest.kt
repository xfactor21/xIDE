package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class DeveloperQueryTest {

    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var vfs: VirtualFileSystem
    private lateinit var fakeFs: FakeFileSystemProvider
    private lateinit var engine: CodeIntelligenceEngine
    private lateinit var navigator: CodeNavigator
    private lateinit var diagnosticEngine: FakeDiagnosticsEngine
    private lateinit var diagnosticAnalyzer: DiagnosticAnalyzer
    private lateinit var explanationService: CodeExplanationService
    private lateinit var router: QueryRouter
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("query-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        fakeFs = FakeFileSystemProvider()
        indexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fakeFs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        engine = CodeIntelligenceEngineImpl(indexer, vfs, null, null)
        navigator = CodeNavigator(indexer)
        diagnosticEngine = FakeDiagnosticsEngine()
        diagnosticAnalyzer = DiagnosticAnalyzer(diagnosticEngine)
        explanationService = CodeExplanationService(vfs, engine)

        router = QueryRouter(engine, navigator, diagnosticAnalyzer, explanationService, null)
    }

    @Test
    fun testRoutingBuildFailureQuery() = runBlocking {
        // Set up active build diagnostic in engine
        diagnosticEngine.addDiagnostic(
            BuildDiagnostic(
                category = "compiler",
                severity = DiagnosticSeverity.ERROR,
                message = "Syntax error at MainActivity.kt line 10"
            )
        )

        val query = DeveloperQuery("Why is my build failing?", tempDir.absolutePath)
        val response = router.routeQuery(query)

        assertTrue(response is QueryResponse.BuildAnalysis)
        val analysis = response as QueryResponse.BuildAnalysis
        assertTrue(analysis.message.contains("Discovered"))
        assertEquals(1, analysis.diagnosticGroups.size)
        assertTrue(analysis.diagnosticGroups.first().primaryError.contains("Syntax error"))
    }

    @Test
    fun testRoutingExplainFunctionQuery() = runBlocking {
        val query = DeveloperQuery("Explain this function calculateSum", tempDir.absolutePath)
        val response = router.routeQuery(query)

        assertTrue(response is QueryResponse.CodeExplaining)
        val explanation = (response as QueryResponse.CodeExplaining).explanation
        assertTrue(explanation.purpose.contains("calculateSum"))
    }

    private class FakeDiagnosticsEngine : DiagnosticsEngine {
        private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
        override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = _activeDiagnostics.asStateFlow()

        override fun addDiagnostic(diagnostic: BuildDiagnostic) {
            _activeDiagnostics.value = _activeDiagnostics.value + diagnostic
        }

        override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {
            _activeDiagnostics.value = _activeDiagnostics.value + diagnostics
        }

        override fun clearDiagnostics() {
            _activeDiagnostics.value = emptyList()
        }

        override fun getFormattedDiagnosticsForContext(): List<String> = emptyList()
    }

    private class FakeFileSystemProvider : FileSystemProvider {
        override val protocol: String = "fake"
        override val providerId: String = "vfs.fake"
        override val providerName: String = "Fake FS"
        override val providerVersion: String = "1.0.0"
        override val supportedFeatures: List<String> = listOf("read", "write", "delete", "exists")
        override val requirements: List<String> = emptyList()
        override val limitations: List<String> = emptyList()
        override val description: String = ""
        override val author: String = ""
        override val compatibilityVersion: String = "1.0.0"

        private val files = mutableMapOf<String, ByteArray>()

        override suspend fun healthCheck() = ProviderHealth.HEALTHY
        override suspend fun initialize() {}
        override suspend fun shutdown() {}

        override suspend fun list(path: String): List<FileNode> {
            return files.keys.filter { it.startsWith(path) }.map {
                FileNode(it, it.substringAfterLast('/'), false, 0L, files[it]?.size?.toLong() ?: 0L)
            }
        }

        override suspend fun readFile(path: String): InputStream {
            val bytes = files[path] ?: throw java.io.FileNotFoundException()
            return ByteArrayInputStream(bytes)
        }

        override suspend fun writeFile(path: String): OutputStream {
            return object : ByteArrayOutputStream() {
                override fun close() {
                    super.close()
                    files[path] = toByteArray()
                }
            }
        }

        override suspend fun delete(path: String): Boolean {
            return files.remove(path) != null
        }

        override suspend fun mkdir(path: String): Boolean = true

        override suspend fun exists(path: String): Boolean {
            return files.containsKey(path)
        }
    }
}
