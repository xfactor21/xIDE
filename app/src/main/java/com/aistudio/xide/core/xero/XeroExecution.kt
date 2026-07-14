package com.aistudio.xide.core.xero

import com.aistudio.xide.core.intelligence.ProjectContext

/**
 * Prepares the execution context for Xero to formulate tasks.
 */
data class XeroExecutionContext(
    val context: ProjectContext,
    val activeTasks: List<String>,
    val recentBuildStatus: Boolean?
)

/**
 * A request for Xero to perform or generate an automation task.
 */
data class XeroTaskRequest(
    val id: String,
    val executionContext: XeroExecutionContext,
    val objective: String
)

/**
 * The resulting commands or actions Xero proposes based on the request.
 */
data class XeroTaskResult(
    val requestId: String,
    val summary: String,
    val proposedCommands: List<String> // References to CommandSystem commands
)
