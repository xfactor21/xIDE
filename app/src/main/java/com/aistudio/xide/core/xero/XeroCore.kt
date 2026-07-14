package com.aistudio.xide.core.xero

/**
 * Models the project context that Xero is aware of.
 */
data class XeroProjectContext(
    val name: String,
    val rootPath: String,
    val activeFile: String?,
    val recentChanges: List<String>,
    val buildStatus: String,
    val diagnostics: List<String>
)

/**
 * Xero Core Intelligence Layer - The AI Engineering Agent.
 */
interface XeroCore {
    /**
     * Initializes Xero with the context of a specific project.
     */
    suspend fun attachToProject(projectPath: String)
    
    /**
     * Asks Xero to analyze a problem and propose a plan.
     */
    suspend fun analyze(prompt: String, contextFiles: List<String>): AnalysisPlan
    
    /**
     * Authorizes Xero to execute a generated plan.
     */
    suspend fun execute(plan: AnalysisPlan): ExecutionResult

    /**
     * Retrieves the project context Xero is currently aware of.
     */
    suspend fun getXeroProjectContext(): XeroProjectContext
}


data class AnalysisPlan(
    val id: String,
    val summary: String,
    val proposedActions: List<XeroAction>
)

sealed class XeroAction {
    data class CreateFile(val path: String, val content: String) : XeroAction()
    data class EditFile(val path: String, val instruction: String) : XeroAction()
    data class RunCommand(val command: String) : XeroAction()
}

sealed class ExecutionResult {
    object Success : ExecutionResult()
    data class PartialSuccess(val completed: Int, val total: Int, val lastError: Throwable) : ExecutionResult()
    data class Failure(val error: Throwable) : ExecutionResult()
}
