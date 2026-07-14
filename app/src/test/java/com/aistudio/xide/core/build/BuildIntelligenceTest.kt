package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.*
import com.aistudio.xide.core.automation.*
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BuildIntelligenceTest {

    @Test
    fun testBuildRequestCreation() {
        val request = BuildRequest(
            target = "app",
            variant = "debug",
            operation = "assembleDebug",
            metadata = mapOf("initiator" to "Xero")
        )

        assertEquals("app", request.target)
        assertEquals("debug", request.variant)
        assertEquals("assembleDebug", request.operation)
        assertEquals("Xero", request.metadata["initiator"])
    }

    @Test
    fun testBuildResultAndDiagnostics() {
        val location = DiagnosticLocation(
            filePath = "/src/Main.kt",
            line = 42,
            column = 15
        )

        val compilerDiag = CompilerDiagnostic(
            code = "UNRESOLVED_REFERENCE",
            message = "Unresolved reference: xide",
            severity = DiagnosticSeverity.ERROR,
            location = location
        )

        val buildDiag = BuildDiagnostic(
            category = "compiler",
            severity = DiagnosticSeverity.ERROR,
            message = "Unresolved reference: xide",
            compilerDiagnostic = compilerDiag,
            rawOutput = "e: /src/Main.kt: (42, 15): Unresolved reference: xide"
        )

        val artifact = ArtifactInfo(
            path = "/build/app-debug.apk",
            timestamp = 123456789L,
            variant = "debug"
        )

        val result = BuildResult(
            success = false,
            artifactInfo = artifact,
            diagnostics = listOf(buildDiag),
            startTime = 1000L,
            endTime = 2000L,
            message = "Build failed due to unresolved reference"
        )

        assertFalse(result.success)
        assertNotNull(result.artifactInfo)
        assertEquals("/build/app-debug.apk", result.artifactInfo?.path)
        assertEquals(1, result.diagnostics.size)
        assertEquals(DiagnosticSeverity.ERROR, result.diagnostics[0].severity)
        assertEquals("compiler", result.diagnostics[0].category)
        assertEquals(42, result.diagnostics[0].compilerDiagnostic?.location?.line)
    }

    @Test
    fun testDiagnosticsEngineImpl() {
        runBlocking {
            val engine = DiagnosticsEngineImpl()
            assertTrue(engine.activeDiagnostics.value.isEmpty())

            val location = DiagnosticLocation("/src/Main.kt", 10, 5)
            val compilerDiag = CompilerDiagnostic(null, "Unused import", DiagnosticSeverity.WARNING, location)
            val diag = BuildDiagnostic("compiler", DiagnosticSeverity.WARNING, "Unused import", compilerDiag)

            engine.addDiagnostic(diag)
            assertEquals(1, engine.activeDiagnostics.value.size)

            val formatted = engine.getFormattedDiagnosticsForContext()
            assertEquals(1, formatted.size)
            assertEquals("[WARNING] Unused import at /src/Main.kt line 10 col 5", formatted[0])

            engine.clearDiagnostics()
            assertTrue(engine.activeDiagnostics.value.isEmpty())
        }
    }

    @Test
    fun testGradleBuildProviderBoundaryAndFailedBuild() {
        runBlocking {
            val tempDir = System.getProperty("java.io.tmpdir")
            val testProjectDir = File(tempDir, "xide_test_project_build")
            if (testProjectDir.exists()) testProjectDir.deleteRecursively()
            testProjectDir.mkdirs()

            val provider = GradleBuildProvider { testProjectDir.absolutePath }
            
            // Assert DEGRADED health when gradle wrapper is missing
            assertEquals(ProviderHealth.DEGRADED, provider.healthCheck())

            val request = BuildRequest("app", "debug", "assembleDebug")
            
            // Test environment validation and failed build reporting on missing wrapper
            val result = provider.executeBuild(request)
            assertFalse(result.success)
            assertNull(result.artifactInfo)
            assertEquals(1, result.diagnostics.size)
            assertEquals("environment", result.diagnostics[0].category)
            assertTrue(result.message.contains("Missing Gradle wrapper") || result.message.contains("incomplete"))

            testProjectDir.deleteRecursively()
        }
    }

    @Test
    fun testBuildAutomationProviderAndPermissions() {
        runBlocking {
            val tempDir = System.getProperty("java.io.tmpdir")
            val testProjectDir = File(tempDir, "xide_test_project_auth")
            if (testProjectDir.exists()) testProjectDir.deleteRecursively()
            testProjectDir.mkdirs()

            val diagnosticsEngine = DiagnosticsEngineImpl()
            val buildProvider = GradleBuildProvider { testProjectDir.absolutePath }
            val automationProvider = BuildAutomationProvider(buildProvider, diagnosticsEngine)

            assertTrue(automationProvider.canExecute(AutomationAction.RunBuild("app", "debug")))
            assertFalse(automationProvider.canExecute(AutomationAction.CreateFile("/some/file.kt", "")))

            // Test action dispatch with failed wrapper check
            val actionResult = automationProvider.executeAction(AutomationAction.RunBuild("app", "debug"))
            assertFalse(actionResult.success)
            assertTrue(actionResult.message.contains("Missing Gradle wrapper") || actionResult.message.contains("incomplete"))

            // Diagnostics should have been streamed to the central DiagnosticsEngine
            assertEquals(1, diagnosticsEngine.activeDiagnostics.value.size)
            assertEquals("environment", diagnosticsEngine.activeDiagnostics.value[0].category)

            testProjectDir.deleteRecursively()
        }
    }
}
