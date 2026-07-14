package com.aistudio.xide.core.automation

import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.xero.AnalysisPlan
import com.aistudio.xide.core.xero.ExecutionResult
import com.aistudio.xide.core.xero.XeroAction
import com.aistudio.xide.core.xero.XeroCoreImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutomationEngineTest {

    private class FakeTaskQueue : TaskQueue {
        override fun enqueue(task: AutomationTask) {}
        override fun cancel(taskId: String) {}
        override fun observeTaskStatus(taskId: String) = kotlinx.coroutines.flow.MutableStateFlow(TaskStatus.QUEUED)
        override fun observeTaskProgress(taskId: String) = kotlinx.coroutines.flow.MutableStateFlow(0f)
    }

    private class FakeXeroMemoryContext : com.aistudio.xide.core.xero.XeroMemoryContext {
        override suspend fun getMemory(): com.aistudio.xide.core.xero.XeroMemory {
            return com.aistudio.xide.core.xero.XeroMemory(emptyMap(), emptyList(), emptyMap(), emptyMap())
        }
        override suspend fun recordAction(action: String) {}
        override suspend fun recordSolution(problem: String, solution: String) {}
    }

    @Test
    fun testPermissionVerifierAllowedPath() {
        val verifier = AutomationPermissionVerifierImpl { "/workspace/myproject" }
        val verdict = verifier.isPermitted(AutomationAction.CreateFile("/workspace/myproject/src/Main.kt", ""))
        assertTrue(verdict is PermissionVerdict.Allowed)
    }

    @Test
    fun testPermissionVerifierDeniedPath() {
        val verifier = AutomationPermissionVerifierImpl { "/workspace/myproject" }
        val verdict = verifier.isPermitted(AutomationAction.CreateFile("/etc/shadow", ""))
        assertTrue(verdict is PermissionVerdict.Denied)
        assertEquals("Path is outside active workspace boundary: /etc/shadow", (verdict as PermissionVerdict.Denied).reason)
    }

    @Test
    fun testPermissionVerifierGradleCommand() {
        val verifier = AutomationPermissionVerifierImpl { "/workspace/myproject" }
        val verdict = verifier.isPermitted(AutomationAction.ExecuteCommand("./gradlew build"))
        assertTrue(verdict is PermissionVerdict.Allowed)
    }

    @Test
    fun testPermissionVerifierDeniedCommand() {
        val verifier = AutomationPermissionVerifierImpl { "/workspace/myproject" }
        val verdict = verifier.isPermitted(AutomationAction.ExecuteCommand("rm -rf /"))
        assertTrue(verdict is PermissionVerdict.Denied)
        assertTrue((verdict as PermissionVerdict.Denied).reason.contains("Arbitrary command execution is forbidden"))
    }

    @Test
    fun testFileSystemAutomationProvider() {
        runBlocking {
            val provider = FileSystemAutomationProviderImpl()
            val tempDir = System.getProperty("java.io.tmpdir")
            val testFile = java.io.File(tempDir, "xide_test_Main.kt")
            if (testFile.exists()) testFile.delete()

            assertTrue(provider.canExecute(AutomationAction.CreateFile(testFile.absolutePath, "")))
            
            // Test real create
            val result = provider.executeAction(AutomationAction.CreateFile(testFile.absolutePath, "fun main() {}"))
            assertTrue(result.success)
            assertTrue(testFile.exists())
            assertEquals("fun main() {}", testFile.readText())

            // Test real update
            val updateResult = provider.executeAction(AutomationAction.ModifyFile(testFile.absolutePath, "fun main()", "fun main(args: Array<String>)"))
            assertTrue(updateResult.success)
            assertEquals("fun main(args: Array<String>) {}", testFile.readText())

            testFile.delete()
        }
    }

    @Test
    fun testTerminalAutomationProvider() {
        runBlocking {
            val provider = TerminalAutomationProviderImpl()
            assertTrue(provider.canExecute(AutomationAction.RunBuild("project-1", "debug")))
            val result = provider.executeAction(AutomationAction.RunBuild("project-1", "debug"))
            assertFalse(result.success)
            assertTrue(result.message.contains("explicitly unfinished boundary"))
        }
    }

    @Test
    fun testActionPlannerMapping() {
        val planner = ActionPlannerImpl()
        val plan = AnalysisPlan(
            id = "plan-123",
            summary = "Recommendation summary",
            proposedActions = listOf(
                XeroAction.CreateFile("/workspace/File.kt", "content"),
                XeroAction.EditFile("/workspace/File.kt", "instruction"),
                XeroAction.RunCommand("gradle assembleDebug")
            )
        )

        val actions = planner.planActions(plan)
        assertEquals(3, actions.size)
        assertTrue(actions[0] is AutomationAction.CreateFile)
        assertTrue(actions[1] is AutomationAction.ModifyFile)
        assertTrue(actions[2] is AutomationAction.ExecuteCommand)
    }

    @Test
    fun testAutomationEngineDispatch() {
        runBlocking {
            val taskQueue = FakeTaskQueue()
            val tempDir = System.getProperty("java.io.tmpdir")
            val verifier = AutomationPermissionVerifierImpl { tempDir }
            val fsProvider = FileSystemAutomationProviderImpl()
            val termProvider = TerminalAutomationProviderImpl()
            
            val engine = AutomationEngineImpl(
                taskQueue = taskQueue,
                permissionVerifier = verifier,
                providers = listOf(fsProvider, termProvider)
            )

            val targetFile = java.io.File(tempDir, "src/Main.kt")
            if (targetFile.exists()) targetFile.delete()

            val result = engine.dispatchAction(AutomationAction.CreateFile(targetFile.absolutePath, "content"))
            assertTrue(result.success)
            assertTrue(result.message.contains("File successfully created"))
            assertTrue(targetFile.exists())

            targetFile.delete()
        }
    }

    @Test
    fun testXeroCoreIntegrationWithAutomation() {
        runBlocking {
            val taskQueue = FakeTaskQueue()
            val tempDir = System.getProperty("java.io.tmpdir")
            val verifier = AutomationPermissionVerifierImpl { tempDir }
            val fsProvider = FileSystemAutomationProviderImpl()
            val termProvider = TerminalAutomationProviderImpl()
            
            val engine = AutomationEngineImpl(
                taskQueue = taskQueue,
                permissionVerifier = verifier,
                providers = listOf(fsProvider, termProvider)
            )

            val planner = ActionPlannerImpl()
            val memoryContext = FakeXeroMemoryContext()

            val targetFile = java.io.File(tempDir, "src/Main.kt")
            if (targetFile.exists()) targetFile.delete()

            val plan = AnalysisPlan(
                id = "plan-123",
                summary = "Test summary",
                proposedActions = listOf(
                    XeroAction.CreateFile(targetFile.absolutePath, "content")
                )
            )

            // Mock dependencies for XeroCoreImpl
            val mockAiProvider = object : com.aistudio.xide.core.ai.AiProvider {
                override val providerId = "test-ai"
                override val providerName = "Test AI"
                override val providerVersion = "1.0"
                override val supportedFeatures = emptyList<String>()
                override val requirements = emptyList<String>()
                override val limitations = emptyList<String>()
                override val description = "desc"
                override val author = "author"
                override val compatibilityVersion = "1.0"
                override suspend fun initialize() {}
                override suspend fun shutdown() {}
                override suspend fun healthCheck() = ProviderHealth.HEALTHY
                override suspend fun execute(request: com.aistudio.xide.core.ai.AiRequest): com.aistudio.xide.core.ai.AiResponse {
                    return com.aistudio.xide.core.ai.AiResponse.Success("Text")
                }
                override fun supportsCapability(capability: String) = true
            }

            val mockAiRouter = com.aistudio.xide.core.ai.AiCapabilityRouterImpl().apply {
                registerProvider(mockAiProvider)
            }
            val mockContextManager = com.aistudio.xide.core.ai.AiContextManagerImpl(null) { tempDir }
            val mockAiService = com.aistudio.xide.core.ai.AiServiceImpl(mockAiRouter, mockContextManager)

            val xeroCore = XeroCoreImpl(
                aiService = mockAiService,
                aiContextManager = mockContextManager,
                projectIndexer = null,
                projectStructureService = null,
                memoryContext = memoryContext,
                actionPlanner = planner,
                automationService = engine
            )

            val result = xeroCore.execute(plan)
            assertTrue(result is ExecutionResult.Success)
            assertTrue(targetFile.exists())
            assertEquals("content", targetFile.readText())

            targetFile.delete()
        }
    }
}
