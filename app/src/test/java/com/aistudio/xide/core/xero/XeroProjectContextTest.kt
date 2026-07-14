package com.aistudio.xide.core.xero

import com.aistudio.xide.core.ai.AiContextManagerImpl
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class XeroProjectContextTest {

    @Test
    fun testProjectInformationReachesAiContext() = runBlocking {
        val indexer = ProjectIndexerImpl()
        val tempDir = File.createTempFile("xero-project-context-test", "")
        tempDir.delete()
        tempDir.mkdirs()
        
        File(tempDir, "MainActivity.kt").writeText("class MainActivity")

        indexer.indexProject(tempDir.absolutePath).await()

        val contextManager = AiContextManagerImpl(
            projectIndexer = indexer,
            activeProjectRootProvider = { tempDir.absolutePath },
            diagnosticsEngine = DiagnosticsEngineImpl()
        )

        val snapshot = contextManager.captureCurrentContext()
        assertNotNull(snapshot.projectContext)
        assertEquals(tempDir.absolutePath, snapshot.projectContext?.projectPath)
        assertTrue(snapshot.activeFilePaths.any { it.endsWith("MainActivity.kt") })
    }

    @Test
    fun testSecretsAreFiltered() = runBlocking {
        val engine = DiagnosticsEngineImpl()
        val secretDiagnostic = BuildDiagnostic(
            category = "compiler",
            severity = DiagnosticSeverity.ERROR,
            message = "Authentication failure with api_key=\"AIzaSyD-unclean-token-secret-123456\"",
            compilerDiagnostic = null,
            rawOutput = "api_key=AIzaSyD-unclean-token-secret-123456"
        )
        engine.addDiagnostic(secretDiagnostic)

        val formattedList = engine.getFormattedDiagnosticsForContext()
        assertFalse(formattedList.any { it.contains("AIzaSyD-unclean-token") })
        assertTrue(formattedList.any { it.contains("[REDACTED_SECRET]") })
    }
}
