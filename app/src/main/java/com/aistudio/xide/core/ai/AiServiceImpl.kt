package com.aistudio.xide.core.ai

/**
 * Concrete implementation of the high-level decoupled AI service.
 */
class AiServiceImpl(
    private val capabilityRouter: AiCapabilityRouter,
    private val contextManager: AiContextManager
) : AiService {

    override suspend fun generateCode(prompt: String, context: AiContextSnapshot): AiServiceResult {
        return executeHighLevelRequest(
            prompt = "Generate code based on prompt:\n$prompt",
            context = context,
            requiredCapability = "code_generation",
            systemInstruction = "You are the Lead xIDE AI System. Generate clean, robust, and highly production-grade Kotlin/Compose code."
        )
    }

    override suspend fun analyzeDiagnostics(diagnostics: List<String>, context: AiContextSnapshot): AiServiceResult {
        val diagnosticsJoined = diagnostics.joinToString("\n") { "- $it" }
        return executeHighLevelRequest(
            prompt = "Analyze the following workspace diagnostics and suggest solutions:\n$diagnosticsJoined",
            context = context,
            requiredCapability = "error_analysis",
            systemInstruction = "You are the Lead xIDE Diagnostics Engineer. Pinpoint structural issues and explain clear remediation paths."
        )
    }

    override suspend fun explainCode(code: String, context: AiContextSnapshot): AiServiceResult {
        return executeHighLevelRequest(
            prompt = "Explain this code:\n```kotlin\n$code\n```",
            context = context,
            requiredCapability = "code_explanation",
            systemInstruction = "You are the Lead xIDE Technical Architect. Explain the structural pattern, time complexity, and dataflow clearly."
        )
    }

    private suspend fun executeHighLevelRequest(
        prompt: String,
        context: AiContextSnapshot,
        requiredCapability: String,
        systemInstruction: String
    ): AiServiceResult {
        try {
            // Build unified context content
            val contextString = buildString {
                append("### Workspace Context Snapshot\n")
                context.projectContext?.let {
                    append("Project Type: ${it.projectType}\n")
                    append("Languages: ${it.languages.joinToString(", ")}\n")
                    append("Frameworks: ${it.frameworks.joinToString(", ")}\n")
                    append("Build System: ${it.buildSystem}\n")
                }
                if (context.activeFilePaths.isNotEmpty()) {
                    append("Active Files:\n")
                    context.activeFilePaths.forEach { append("  - $it\n") }
                }
                if (context.buildDiagnostics.isNotEmpty()) {
                    append("Active Diagnostics:\n")
                    context.buildDiagnostics.forEach { append("  - $it\n") }
                }
                if (context.customContextMap.isNotEmpty()) {
                    append("Additional Details:\n")
                    context.customContextMap.forEach { (k, v) -> append("  $k: $v\n") }
                }
            }

            val messages = listOf(
                AiChatMessage(role = AiRole.SYSTEM, content = "Context Details:\n$contextString"),
                AiChatMessage(role = AiRole.USER, content = prompt)
            )

            val request = AiRequest(
                messages = messages,
                systemInstruction = systemInstruction,
                requiredCapability = requiredCapability
            )

            // Dynamic route to matched provider
            val provider = capabilityRouter.route(request)
            
            // Execute request
            return when (val response = provider.execute(request)) {
                is AiResponse.Success -> AiServiceResult.Success(response.text)
                is AiResponse.Failure -> AiServiceResult.Failure(response.error)
            }
        } catch (e: Exception) {
            return AiServiceResult.Failure(e)
        }
    }
}
