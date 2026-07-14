package com.aistudio.xide.core.ai

import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.intelligence.ProjectIndexer
import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.xero.XeroCoreImpl
import com.aistudio.xide.core.xero.XeroMemoryContext
import com.aistudio.xide.core.xero.XeroMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AiIntelligenceTest {

    private class FakeAiProvider(
        override val providerId: String,
        private val supportedCaps: List<String>,
        private val responseText: String
    ) : AiProvider {
        override val providerName: String = "Fake AI Provider"
        override val providerVersion: String = "1.0"
        override val supportedFeatures: List<String> = supportedCaps
        override val requirements: List<String> = emptyList()
        override val limitations: List<String> = emptyList()
        override val description: String = "Fake for unit test"
        override val author: String = "xIDE Developer"
        override val compatibilityVersion: String = "1.0"

        override suspend fun initialize() {}
        override suspend fun shutdown() {}
        override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY

        override suspend fun execute(request: AiRequest): AiResponse {
            return AiResponse.Success(
                text = responseText,
                usageMetadata = TokenUsage(10, 20, 30),
                modelName = "fake-llm-1.0"
            )
        }

        override fun supportsCapability(capability: String): Boolean {
            return supportedCaps.contains(capability)
        }
    }

    private class FakeXeroMemoryContext : XeroMemoryContext {
        val actions = mutableListOf<String>()
        override suspend fun getMemory(): XeroMemory = XeroMemory(emptyMap(), actions, emptyMap(), emptyMap())
        override suspend fun recordAction(action: String) { actions.add(action) }
        override suspend fun recordSolution(problem: String, solution: String) {}
    }

    private class FakeProjectIndexer : ProjectIndexer {
        override fun indexProject(projectPath: String): com.aistudio.xide.core.intelligence.IndexingTask {
            return object : com.aistudio.xide.core.intelligence.IndexingTask {
                override val taskId: String = "task-1"
                override val projectPath: String = projectPath
                override val progress: StateFlow<Float> = MutableStateFlow(1.0f)
                override val status: StateFlow<com.aistudio.xide.core.intelligence.IndexingStatus> = 
                    MutableStateFlow(com.aistudio.xide.core.intelligence.IndexingStatus.COMPLETED)
                override suspend fun await() {}
                override fun cancel() {}
            }
        }

        override suspend fun getIndex(projectPath: String): com.aistudio.xide.core.intelligence.ProjectIndex? = null
        override suspend fun getContext(projectPath: String): ProjectContext {
            return ProjectContext(
                projectPath = projectPath,
                activeFiles = listOf("Main.kt"),
                architectureSummary = "Jetpack Compose",
                contextPreparation = "",
                projectType = "android_app",
                languages = listOf("kotlin"),
                frameworks = listOf("compose"),
                buildSystem = "gradle",
                dependencies = listOf("androidx.compose:compose-bom"),
                activeProblems = listOf("Unresolved reference"),
                recommendedActions = listOf("Import symbol")
            )
        }
    }

    @Test
    fun testCapabilityRoutingRegistry() {
        val router = AiCapabilityRouterImpl()
        val provider = FakeAiProvider("p1", listOf("code_generation"), "result")

        router.registerProvider(provider)
        assertEquals(1, router.getRegisteredProviders().size)
        assertEquals(provider, router.getRegisteredProviders().first())

        router.unregisterProvider("p1")
        assertEquals(0, router.getRegisteredProviders().size)
    }

    @Test
    fun testSuccessfulCapabilityRouting() = runBlocking {
        val router = AiCapabilityRouterImpl()
        val provider = FakeAiProvider("p1", listOf("code_generation"), "result")
        router.registerProvider(provider)

        val request = AiRequest(
            messages = emptyList(),
            requiredCapability = "code_generation"
        )

        val resolved = router.route(request)
        assertEquals("p1", resolved.providerId)
    }

    @Test
    fun testCapabilityRoutingFailureWhenNoMatch() = runBlocking {
        val router = AiCapabilityRouterImpl()
        val provider = FakeAiProvider("p1", listOf("code_generation"), "result")
        router.registerProvider(provider)

        val request = AiRequest(
            messages = emptyList(),
            requiredCapability = "image_generation"
        )

        try {
            router.route(request)
            fail("Should have thrown NoSuitableAiProviderException")
        } catch (e: NoSuitableAiProviderException) {
            assertTrue(e.message!!.contains("No healthy registered AI provider supports the required capability"))
        }
    }

    @Test
    fun testAiServiceExecution() = runBlocking {
        val router = AiCapabilityRouterImpl()
        val provider = FakeAiProvider("p1", listOf("code_generation"), "Generated Code Output")
        router.registerProvider(provider)

        val contextManager = AiContextManagerImpl(null) { "/workspace" }
        val aiService = AiServiceImpl(router, contextManager)

        val snapshot = contextManager.captureCurrentContext()
        val result = aiService.generateCode("Write greeting", snapshot)

        assertTrue(result is AiServiceResult.Success)
        assertEquals("Generated Code Output", (result as AiServiceResult.Success).content)
    }

    @Test
    fun testXeroCoreOrchestration() = runBlocking {
        val router = AiCapabilityRouterImpl()
        val provider = FakeAiProvider("p1", listOf("code_generation"), "Generated Code Output")
        router.registerProvider(provider)

        val projectIndexer = FakeProjectIndexer()
        val contextManager = AiContextManagerImpl(projectIndexer) { "/workspace" }
        val aiService = AiServiceImpl(router, contextManager)
        val memoryContext = FakeXeroMemoryContext()

        val xeroCore = XeroCoreImpl(
            aiService = aiService,
            aiContextManager = contextManager,
            projectIndexer = projectIndexer,
            projectStructureService = null,
            memoryContext = memoryContext
        )

        xeroCore.attachToProject("/workspace")
        assertTrue(memoryContext.actions.any { it.contains("Attaching Xero core") })

        val plan = xeroCore.analyze("create some files", emptyList())
        assertNotNull(plan)
        assertTrue(plan.summary.contains("Generated Code Output"))
        assertEquals(1, plan.proposedActions.size)
        
        val action = plan.proposedActions.first()
        assertTrue(action is com.aistudio.xide.core.xero.XeroAction.CreateFile)
    }
}
