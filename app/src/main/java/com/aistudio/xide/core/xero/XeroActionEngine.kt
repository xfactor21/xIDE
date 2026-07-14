package com.aistudio.xide.core.xero

import com.aistudio.xide.core.approval.ApprovalManager
import com.aistudio.xide.core.command.ActionDescriptor
import com.aistudio.xide.core.command.CommandSystem
import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.intelligence.LanguageModelProvider
import com.aistudio.xide.core.intelligence.StructuredOutputValidator
import com.aistudio.xide.core.intelligence.ModelRequest
import com.aistudio.xide.core.intelligence.ModelResponse
import com.aistudio.xide.core.intelligence.ActionPlan
import com.aistudio.xide.core.intelligence.MalformedOutputException
import com.aistudio.xide.core.intelligence.HallucinatedCapabilityException
import com.aistudio.xide.core.intelligence.UnsafeActionException
import com.aistudio.xide.core.service.ServiceRegistry

class UnavailableProviderException(message: String) : Exception(message)
class InvalidPermissionException(message: String) : Exception(message)

/**
 * Transforms reasoning output into executable workflows via CommandSystem.
 * Coordinates the live LLM, Structured Validation, and Approval pipelines.
 */
class XeroActionEngine(
    private val commandSystem: CommandSystem,
    private val approvalManager: ApprovalManager,
    private val memoryContext: XeroMemoryContext,
    private val modelProvider: LanguageModelProvider,
    private val outputValidator: StructuredOutputValidator,
    private val serviceRegistry: ServiceRegistry
) {

    /**
     * Executes the live AI orchestration pipeline:
     * LLM -> ActionPlan validation -> Provider checks -> Permission checks -> ApprovalManager
     */
    suspend fun executeTaskFlow(objective: String, projectContext: ProjectContext): ActionPlan {
        // Record action in persistent memory
        memoryContext.recordAction("Starting task flow for objective: $objective")

        val availableCapabilities = serviceRegistry.getAllCapabilities()
        val activeCapabilities = serviceRegistry.getActiveCapabilities()
        val capabilitiesString = if (availableCapabilities.isNotEmpty()) {
            availableCapabilities.joinToString(" | ") { "\"$it\"" }
        } else {
            "\"automation.build_project\" | \"automation.run_terminal\""
        }

        // 1. Prepare Model Prompt
        val prompt = """
            Objective: $objective
            Project Path: ${projectContext.projectPath}
            Project Type: ${projectContext.projectType}
            Build System: ${projectContext.buildSystem}
            
            Return a single JSON block representing an ActionPlan conforming to the schema below. Do NOT wrap it in any HTML or text. Just raw JSON.
            
            Schema:
            {
              "title": "Action Plan Title",
              "commands": [
                {
                  "commandId": $capabilitiesString,
                  "description": "Short description",
                  "arguments": { "key": "value" }
                }
              ],
              "requiresApproval": true
            }
        """.trimIndent()

        val request = ModelRequest(
            prompt = prompt,
            systemInstruction = "You are Xero, xIDE's advanced autonomous AI developer assistant. You plan tasks carefully and propose structured actions safely."
        )

        // 2. Call the LLM
        val response = modelProvider.generate(request)
        if (response is ModelResponse.Failure) {
            memoryContext.recordAction("LLM Call Failed: ${response.error.message}")
            throw response.error
        }

        val jsonText = (response as ModelResponse.Success).text

        // 3. Deserialize and Validate the JSON ActionPlan
        val plan = try {
            outputValidator.validateAndParse(jsonText, availableCapabilities)
        } catch (e: MalformedOutputException) {
            memoryContext.recordAction("Failed validation: Malformed Output")
            throw e
        } catch (e: HallucinatedCapabilityException) {
            memoryContext.recordAction("Failed validation: Hallucinated Capability")
            throw e
        } catch (e: UnsafeActionException) {
            memoryContext.recordAction("Failed validation: Unsafe Action")
            throw e
        }

        // 4. Failure handling: Unavailable Providers check
        for (cmd in plan.commands) {
            if (!activeCapabilities.contains(cmd.commandId)) {
                throw UnavailableProviderException("The provider for command ID '${cmd.commandId}' is currently offline or unavailable.")
            }
        }

        // 5. Failure handling: Invalid Permissions check
        val activePermissions = serviceRegistry.getAllDeclaredPermissions()
        for (cmd in plan.commands) {
            val requiredPermissions = serviceRegistry.getPermissionsForCapability(cmd.commandId)
            for (requiredPerm in requiredPermissions) {
                if (!activePermissions.contains(requiredPerm)) {
                    throw InvalidPermissionException("Action requires permission '$requiredPerm' which is not granted to Xero.")
                }
            }
        }

        // 6. Request human approval in the loop
        val requiredPermsList = plan.commands.flatMap { cmd ->
            serviceRegistry.getPermissionsForCapability(cmd.commandId)
        }.distinct()

        approvalManager.requestApproval(
            description = plan.title,
            commands = plan.commands,
            permissions = requiredPermsList
        )

        memoryContext.recordAction("Task flow completed. ActionPlan registered for approval: ${plan.title}")
        return plan
    }

    /**
     * Legacy function updated to map results using the structured CommandSystem boundaries.
     */
    suspend fun processTaskResult(taskResult: XeroTaskResult, projectContext: ProjectContext) {
        memoryContext.recordAction("Processing task result: ${taskResult.requestId}")
        
        val actionDescriptors = taskResult.proposedCommands.map { commandString ->
            if (commandString.contains("build", ignoreCase = true)) {
                ActionDescriptor(
                    commandId = "automation.build_project",
                    description = "Build Project",
                    arguments = mapOf("projectId" to "project1", "buildType" to "debug")
                )
            } else {
                ActionDescriptor(
                    commandId = "automation.run_terminal",
                    description = "Run Terminal Command",
                    arguments = mapOf("sessionId" to "xero_session", "command" to commandString)
                )
            }
        }

        if (actionDescriptors.isNotEmpty()) {
            approvalManager.requestApproval(
                description = "Xero proposed actions: ${actionDescriptors.joinToString { it.description }}",
                commands = actionDescriptors,
                permissions = emptyList()
            )
        }
    }
}
