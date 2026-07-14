package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EndToEndBuildFlowTest {

    private class LocalPathBuildProvider(
        private val rootDir: File,
        private val success: Boolean
    ) : BuildProvider {
        override val providerId: String = "local_path_provider"
        override val providerName: String = "Local Path Provider"
        override val providerVersion: String = "1.0"
        override val compatibilityVersion: String = "1.0"
        override val author: String = "Test"
        override val description: String = "Test"
        override val supportedFeatures: List<String> = emptyList()
        override val requirements: List<String> = emptyList()
        override val limitations: List<String> = emptyList()

        override suspend fun initialize() {}
        override suspend fun shutdown() {}
        override suspend fun healthCheck() = ProviderHealth.HEALTHY

        override fun canBuild(request: BuildRequest): Boolean = true
        override suspend fun executeBuild(request: BuildRequest): BuildResult {
            if (success) {
                // Simulate artifact creation
                val outputDir = File(rootDir, "app/build/outputs/apk/debug")
                outputDir.mkdirs()
                val apkFile = File(outputDir, "app-debug.apk")
                apkFile.writeText("simulated compile payload")
            }
            return BuildResult(
                success = success,
                artifactInfo = null,
                diagnostics = emptyList(),
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis() + 50,
                message = if (success) "Build successful" else "Build failed"
            )
        }
    }

    @Test
    fun testEndToEndSuccessfulBuildAndArtifactResolution() {
        runBlocking {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "e2e-project-${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val provider = LocalPathBuildProvider(tempDir, success = true)
            val diagnosticsEngine = DiagnosticsEngineImpl()
            val resolver = ArtifactResolver { tempDir.absolutePath }
            val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

            val request = BuildRequest(
                target = "app",
                variant = "debug",
                operation = "assembleDebug"
            )

            val result = service.executeBuild(request)

            assertTrue(result.success)
            assertEquals(BuildState.SUCCESS, service.buildState.value)

            // Verify that BuildService resolved the artifact and returned it in BuildResult
            assertNotNull(result.artifactInfo)
            val artifact = result.artifactInfo!!
            assertEquals("app-debug.apk", artifact.metadata["filename"])
            assertEquals("debug", artifact.variant)
            assertTrue(File(artifact.path).exists())

            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testEndToEndFailedBuildNoArtifact() {
        runBlocking {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "e2e-project-failed-${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val provider = LocalPathBuildProvider(tempDir, success = false)
            val diagnosticsEngine = DiagnosticsEngineImpl()
            val resolver = ArtifactResolver { tempDir.absolutePath }
            val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

            val request = BuildRequest(
                target = "app",
                variant = "debug",
                operation = "assembleDebug"
            )

            val result = service.executeBuild(request)

            assertFalse(result.success)
            assertEquals(BuildState.FAILED, service.buildState.value)
            assertNull(result.artifactInfo)

            tempDir.deleteRecursively()
        }
    }
}
