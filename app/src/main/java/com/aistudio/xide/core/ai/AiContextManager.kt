package com.aistudio.xide.core.ai

import com.aistudio.xide.core.intelligence.ProjectContext

/**
 * Responsible for managing active workspace/file context to inject into AI requests.
 */
interface AiContextManager {
    /**
     * Gathers the current context snapshot of the workspace.
     */
    suspend fun captureCurrentContext(): AiContextSnapshot

    /**
     * Appends specific file content or diagnostics to the current session context.
     */
    fun addManualContext(key: String, content: String)

    /**
     * Clears manual context entries.
     */
    fun clearManualContext()
}

/**
 * Immutable context snapshot prepared for the AI orchestration layer.
 */
data class AiContextSnapshot(
    val projectContext: ProjectContext?,
    val activeFilePaths: List<String>,
    val recentUserActions: List<String>,
    val buildDiagnostics: List<String>,
    val customContextMap: Map<String, String> = emptyMap()
)
