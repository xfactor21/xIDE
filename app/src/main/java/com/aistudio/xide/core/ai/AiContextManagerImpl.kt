package com.aistudio.xide.core.ai

import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.intelligence.ProjectIndexer
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import java.util.concurrent.ConcurrentHashMap

/**
 * Concrete context manager implementing unified context gathering from the project indexer
 * and manual user session state.
 */
class AiContextManagerImpl(
    private val projectIndexer: ProjectIndexer?,
    private val activeProjectRootProvider: () -> String?,
    private val diagnosticsEngine: DiagnosticsEngine?
) : AiContextManager {

    // Secondary constructor for backward compatibility with trailing lambda usages
    constructor(
        projectIndexer: ProjectIndexer?,
        activeProjectRootProvider: () -> String?
    ) : this(projectIndexer, activeProjectRootProvider, null)

    private val manualContext = ConcurrentHashMap<String, String>()

    override suspend fun captureCurrentContext(): AiContextSnapshot {
        val rootPath = activeProjectRootProvider()
        val projectCtx = if (rootPath != null && projectIndexer != null) {
            projectIndexer.getContext(rootPath)
        } else {
            null
        }

        val parsedDiagnostics = diagnosticsEngine?.getFormattedDiagnosticsForContext() ?: emptyList()
        val indexerDiagnostics = projectCtx?.activeProblems ?: emptyList()

        return AiContextSnapshot(
            projectContext = projectCtx,
            activeFilePaths = projectCtx?.activeFiles ?: emptyList(),
            recentUserActions = emptyList(), // Planned for active telemetry context in future phase
            buildDiagnostics = indexerDiagnostics + parsedDiagnostics,
            customContextMap = manualContext.toMap()
        )
    }

    override fun addManualContext(key: String, content: String) {
        manualContext[key] = content
    }

    override fun clearManualContext() {
        manualContext.clear()
    }
}
