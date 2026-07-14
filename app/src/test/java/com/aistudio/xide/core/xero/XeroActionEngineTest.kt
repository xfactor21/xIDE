package com.aistudio.xide.core.xero

import com.aistudio.xide.core.approval.ApprovalManager
import com.aistudio.xide.core.approval.ApprovalRequest
import com.aistudio.xide.core.approval.ApprovalState
import com.aistudio.xide.core.command.ActionDescriptor
import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult
import com.aistudio.xide.core.command.CommandSystem
import com.aistudio.xide.core.intelligence.ActionPlan
import com.aistudio.xide.core.intelligence.DefaultStructuredOutputValidator
import com.aistudio.xide.core.intelligence.LanguageModelProvider
import com.aistudio.xide.core.intelligence.MalformedOutputException
import com.aistudio.xide.core.intelligence.HallucinatedCapabilityException
import com.aistudio.xide.core.intelligence.UnsafeActionException
import com.aistudio.xide.core.intelligence.ModelRequest
import com.aistudio.xide.core.intelligence.ModelResponse
import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.service.LocalServiceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class XeroActionEngineTest {

    // --- Mock / Fake Implementations for Tests ---

    private class FakeLanguageModelProvider(var responseText: String) : LanguageModelProvider {
        override suspend fun generate(request: ModelRequest): ModelResponse {
            return if (responseText.startsWith("ERROR:")) {
                ModelResponse.Failure(Exception(responseText.removePrefix("ERROR:")))
            } else {
                ModelResponse.Success(responseText)
            }
        }
    }

    private class FakeCommandSystem : CommandSystem {
        val executedCommands = mutableListOf<Command>()
        override suspend fun execute(command: Command): CommandResult {
            executedCommands.add(command)
            return CommandResult.Success
        }
        override suspend fun undoLast() {}
        override suspend fun redoLast() {}
        override val commandHistory: Flow<List<Command>> = MutableStateFlow(emptyList())
    }

    private class FakeApprovalManager : ApprovalManager {
        val requests = mutableListOf<ApprovalRequest>()
        override fun requestApproval(description: String, commands: List<ActionDescriptor>, permissions: List<String>): ApprovalRequest {
            val request = ApprovalRequest(
                requestId = UUID.randomUUID().toString(),
                description = description,
                commands = commands,
                state = ApprovalState.REQUESTED,
                requiredPermissions = permissions
            )
            requests.add(request)
            return request
        }
        override fun approve(requestId: String) {}
        override fun reject(requestId: String) {}
        override fun updateState(requestId: String, state: ApprovalState) {}
        override fun observeRequest(requestId: String): StateFlow<ApprovalState>? = null
        override fun getPendingRequests(): List<ApprovalRequest> = requests
    }

    private class FakeXeroMemoryContext : XeroMemoryContext {
        val actions = mutableListOf<String>()
        val solutions = mutableMapOf<String, String>()
        override suspend fun getMemory(): XeroMemory {
            return XeroMemory(emptyMap(), actions, solutions, emptyMap())
        }
        override suspend fun recordAction(action: String) {
            actions.add(action)
        }
        override suspend fun recordSolution(problem: String, solution: String) {
            solutions[problem] = solution
        }
    }

    // --- Test Cases ---

    @Test
    fun testSuccessfulTaskFlowExecution() = runBlocking {
        val jsonOutput = """
            {
              "title": "Build and Run Test",
              "commands": [
                {
                  "commandId": "automation.build_project",
                  "description": "Trigger debug build",
                  "arguments": { "projectId": "project_1" }
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val modelProvider = FakeLanguageModelProvider(jsonOutput)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.build_project")
        )

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        val plan = engine.executeTaskFlow("Build the active project", context)

        assertNotNull(plan)
        assertEquals("Build and Run Test", plan.title)
        assertEquals(1, plan.commands.size)
        assertEquals("automation.build_project", plan.commands[0].commandId)
        assertTrue(plan.requiresApproval)

        // Verify it was correctly recorded in memory
        assertTrue(memoryContext.actions.any { it.contains("Starting task flow") })
        assertTrue(memoryContext.actions.any { it.contains("Task flow completed") })

        // Verify it registered an approval request
        assertEquals(1, approvalManager.requests.size)
        assertEquals("Build and Run Test", approvalManager.requests[0].description)
    }

    @Test
    fun testFailureMalformedModelOutput() = runBlocking {
        val badJsonOutput = "{ malformed_json: this is bad }"

        val modelProvider = FakeLanguageModelProvider(badJsonOutput)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.build_project")
        )

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        try {
            engine.executeTaskFlow("Build project", context)
            fail("Expected MalformedOutputException was not thrown")
        } catch (e: MalformedOutputException) {
            assertTrue(e.message!!.contains("Failed to deserialize"))
            assertTrue(memoryContext.actions.contains("Failed validation: Malformed Output"))
        }
    }

    @Test
    fun testFailureHallucinatedCapability() = runBlocking {
        val hallucinatedOutput = """
            {
              "title": "Erase Everything Plan",
              "commands": [
                {
                  "commandId": "automation.wipe_everything_now",
                  "description": "A hallucinated command ID",
                  "arguments": {}
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val modelProvider = FakeLanguageModelProvider(hallucinatedOutput)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.build_project")
        )

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        try {
            engine.executeTaskFlow("Erase files", context)
            fail("Expected HallucinatedCapabilityException was not thrown")
        } catch (e: HallucinatedCapabilityException) {
            assertTrue(e.message!!.contains("is not a registered capability"))
            assertTrue(memoryContext.actions.contains("Failed validation: Hallucinated Capability"))
        }
    }

    @Test
    fun testFailureUnsafeActionProposal() = runBlocking {
        val unsafeOutput = """
            {
              "title": "Unsafe Bash Execution",
              "commands": [
                {
                  "commandId": "automation.run_terminal",
                  "description": "Attempting malicious command",
                  "arguments": { "command": "rm -rf /usr/lib" }
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val modelProvider = FakeLanguageModelProvider(unsafeOutput)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.run_terminal")
        )

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        try {
            engine.executeTaskFlow("Execute terminal", context)
            fail("Expected UnsafeActionException was not thrown")
        } catch (e: UnsafeActionException) {
            assertTrue(e.message!!.contains("Unsafe terminal execution pattern detected"))
            assertTrue(memoryContext.actions.contains("Failed validation: Unsafe Action"))
        }
    }

    @Test
    fun testFailureUnavailableProvider() = runBlocking {
        val validJson = """
            {
              "title": "Trigger Build",
              "commands": [
                {
                  "commandId": "automation.build_project",
                  "description": "Trigger debug build",
                  "arguments": { "projectId": "project_1" }
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val modelProvider = FakeLanguageModelProvider(validJson)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.run_terminal")
        )
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "offline.plugin",
            capabilities = listOf("automation.build_project"),
            permissions = emptyList(),
            health = com.aistudio.xide.core.provider.ProviderHealth.FAILING
        )

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        try {
            engine.executeTaskFlow("Run build", context)
            fail("Expected UnavailableProviderException was not thrown")
        } catch (e: UnavailableProviderException) {
            assertTrue(e.message!!.contains("is currently offline or unavailable"))
        }
    }

    @Test
    fun testFailureInvalidPermissions() = runBlocking {
        val validJson = """
            {
              "title": "Write Custom Script",
              "commands": [
                {
                  "commandId": "automation.create_file",
                  "description": "Create safe shell script",
                  "arguments": { "path": "/project/script.sh" }
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val modelProvider = FakeLanguageModelProvider(validJson)
        val validator = DefaultStructuredOutputValidator()
        val commandSystem = FakeCommandSystem()
        val approvalManager = FakeApprovalManager()
        val memoryContext = FakeXeroMemoryContext()
        
        val registry = LocalServiceRegistry()
        // Register the capability with permission "vfs_write"
        registry.register(
            serviceType = String::class.java,
            implementation = "dummy",
            pluginId = "test.plugin",
            capabilities = listOf("automation.create_file"),
            permissions = listOf("vfs_write")
        )
        // Clear active/granted permissions so Xero doesn't have "vfs_write" granted!
        registry.clearPermissions()

        val engine = XeroActionEngine(
            commandSystem = commandSystem,
            approvalManager = approvalManager,
            memoryContext = memoryContext,
            modelProvider = modelProvider,
            outputValidator = validator,
            serviceRegistry = registry
        )

        val context = ProjectContext("/project", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        try {
            engine.executeTaskFlow("Write shell script", context)
            fail("Expected InvalidPermissionException was not thrown")
        } catch (e: InvalidPermissionException) {
            assertTrue(e.message!!.contains("requires permission 'vfs_write' which is not granted"))
        }
    }
}
