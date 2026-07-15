package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuildServiceTest {

    private class FakeBuildProvider(
        private val canBuildResult: Boolean,
        private val buildResult: BuildResult
    ) : BuildProvider {
        override val providerId: String = "fake_provider"
        override val providerName: String = "Fake Provider"
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

        override fun canBuild(request: BuildRequest): Boolean = canBuildResult
        override suspend fun executeBuild(request: BuildRequest): BuildResult = buildResult
    }

    @Test
    fun testBuildServiceSelectsCorrectProvider() = runBlocking {
        val successResult = BuildResult(
            success = true,
            artifactInfo = null,
            diagnostics = emptyList(),
            startTime = 0,
            endTime = 10,
            message = "Build succeeded!"
        )
        val providerMatch = FakeBuildProvider(canBuildResult = true, buildResult = successResult)
        val providerNoMatch = FakeBuildProvider(canBuildResult = false, buildResult = successResult)

        val diagnosticsEngine = DiagnosticsEngineImpl()
        val resolver = ArtifactResolver { "/tmp" }
        val service = BuildServiceImpl(listOf(providerNoMatch, providerMatch), diagnosticsEngine, resolver)

        val request = BuildRequest("app", "debug", "assembleDebug")
        val result = service.executeBuild(request)

        assertTrue(result.success)
        assertEquals("Build succeeded!", result.message)
        assertEquals(BuildState.SUCCESS, service.buildState.value)
    }

    @Test
    fun testBuildServiceNoProviderFound() = runBlocking {
        val diagnosticsEngine = DiagnosticsEngineImpl()
        val resolver = ArtifactResolver { "/tmp" }
        val service = BuildServiceImpl(emptyList(), diagnosticsEngine, resolver)

        val request = BuildRequest("app", "debug", "assembleDebug")
        val result = service.executeBuild(request)

        assertFalse(result.success)
        assertTrue(result.message.contains("No suitable build provider found"))
        assertEquals(BuildState.FAILED, service.buildState.value)
    }

    @Test
    fun testBuildServiceFunnelsDiagnosticsAndSetsFailedState() = runBlocking {
        val diagnostic = BuildDiagnostic(
            category = "compiler",
            severity = DiagnosticSeverity.ERROR,
            message = "Unresolved reference: main"
        )
        val failedResult = BuildResult(
            success = false,
            artifactInfo = null,
            diagnostics = listOf(diagnostic),
            startTime = 0,
            endTime = 10,
            message = "Compilation failed."
        )
        val provider = FakeBuildProvider(canBuildResult = true, buildResult = failedResult)

        val diagnosticsEngine = DiagnosticsEngineImpl()
        val resolver = ArtifactResolver { "/tmp" }
        val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

        val request = BuildRequest("app", "debug", "assembleDebug")
        val result = service.executeBuild(request)

        assertFalse(result.success)
        assertEquals(BuildState.FAILED, service.buildState.value)

        val activeDiags = diagnosticsEngine.activeDiagnostics.value
        assertEquals(1, activeDiags.size)
        assertEquals("Unresolved reference: main", activeDiags[0].message)
    }
}
