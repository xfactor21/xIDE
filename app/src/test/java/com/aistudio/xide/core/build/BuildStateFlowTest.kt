package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BuildStateFlowTest {

    private class ControlledBuildProvider(
        private val buildDeferred: CompletableDeferred<BuildResult>
    ) : BuildProvider {
        override val providerId: String = "controlled_provider"
        override val providerName: String = "Controlled Provider"
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
            return buildDeferred.await()
        }
    }

    @Test
    fun testBuildStateTransitionsToSuccess() {
        runBlocking {
            val buildDeferred = CompletableDeferred<BuildResult>()
            val provider = ControlledBuildProvider(buildDeferred)
            val diagnosticsEngine = DiagnosticsEngineImpl()
            val resolver = ArtifactResolver { "/tmp" }
            val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

            assertEquals(BuildState.IDLE, service.buildState.value)

            val request = BuildRequest("app", "debug", "assembleDebug")
            
            // Execute build asynchronously
            val buildJob = async { service.executeBuild(request) }

            // Yield to let async run and set the state to RUNNING
            yield()

            // State should now be RUNNING
            assertEquals(BuildState.RUNNING, service.buildState.value)

            // Complete the build successfully
            val successResult = BuildResult(
                success = true,
                artifactInfo = null,
                diagnostics = emptyList(),
                startTime = 0,
                endTime = 10,
                message = "Success"
            )
            buildDeferred.complete(successResult)

            val result = buildJob.await()

            assertTrue(result.success)
            assertEquals(BuildState.SUCCESS, service.buildState.value)
        }
    }

    @Test
    fun testBuildStateTransitionsToFailed() {
        runBlocking {
            val buildDeferred = CompletableDeferred<BuildResult>()
            val provider = ControlledBuildProvider(buildDeferred)
            val diagnosticsEngine = DiagnosticsEngineImpl()
            val resolver = ArtifactResolver { "/tmp" }
            val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

            assertEquals(BuildState.IDLE, service.buildState.value)

            val request = BuildRequest("app", "debug", "assembleDebug")
            val buildJob = async { service.executeBuild(request) }

            // Yield to let async run and set the state to RUNNING
            yield()

            assertEquals(BuildState.RUNNING, service.buildState.value)

            // Complete with failure
            val failResult = BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = emptyList(),
                startTime = 0,
                endTime = 10,
                message = "Failure"
            )
            buildDeferred.complete(failResult)

            val result = buildJob.await()

            assertFalse(result.success)
            assertEquals(BuildState.FAILED, service.buildState.value)
        }
    }

    @Test
    fun testBuildStateTransitionsToCancelled() {
        runBlocking {
            val buildDeferred = CompletableDeferred<BuildResult>()
            val provider = ControlledBuildProvider(buildDeferred)
            val diagnosticsEngine = DiagnosticsEngineImpl()
            val resolver = ArtifactResolver { "/tmp" }
            val service = BuildServiceImpl(listOf(provider), diagnosticsEngine, resolver)

            assertEquals(BuildState.IDLE, service.buildState.value)

            val request = BuildRequest("app", "debug", "assembleDebug")
            val buildJob = async { service.executeBuild(request) }

            // Yield to let async run and set the state to RUNNING
            yield()

            assertEquals(BuildState.RUNNING, service.buildState.value)

            // Cancel while running
            service.cancelBuild()
            assertEquals(BuildState.CANCELLED, service.buildState.value)

            // Complete the background deferral
            buildDeferred.complete(BuildResult(false, null, emptyList(), 0, 0, "Interrupted"))
            buildJob.await()
        }
    }
}
