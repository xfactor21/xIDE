package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.workspace.WorkspaceSession
import com.aistudio.xide.core.build.BuildResult
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.ai.actions.AIAction
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.indexing.ProjectIndexerImpl

/**
 * A safe, token-bounded, read-only snapshot representing the full development state of xIDE.
 */
data class XeroDeveloperContext(
    val workspaceSession: WorkspaceSession?,
    val activeFile: String?,
    val cursorLine: Int?,
    val cursorColumn: Int?,
    val selectedCodeRange: String?,
    val recentFileChanges: List<String>,
    val currentDiagnostics: List<BuildDiagnostic>,
    val latestBuildResult: BuildResult?,
    val relevantSymbols: List<String>,
    val recentConversationHistory: List<ConversationEntry>,
    val pendingAiActions: List<AIAction>
) {
    /**
     * Confirms that this context does not expose dangerous properties.
     */
    fun isSecure(): Boolean {
        // Double check no raw secrets are present in any string fields
        val fieldsToScan = listOfNotNull(
            activeFile,
            selectedCodeRange,
            latestBuildResult?.message
        ) + recentFileChanges + relevantSymbols + recentConversationHistory.map { it.question }

        val secretPattern = Regex("(?i)(api_?key|secret|password|token)[\\s:=]+[a-zA-Z0-9_\\-\"\']+")
        val keystorePattern = Regex("(?i)(keystore|private_key)")
        
        for (field in fieldsToScan) {
            if (secretPattern.containsMatchIn(field) && !field.contains("REDACTED")) return false
            if (keystorePattern.containsMatchIn(field) && !field.contains("REDACTED")) return false
        }
        return true
    }
}

/**
 * Interface responsible for assembling a secure, token-bounded, read-only XeroDeveloperContext snapshot.
 */
interface XeroContextProvider {
    suspend fun gatherDeveloperContext(
        projectPath: String,
        activeFile: String? = null,
        cursorLine: Int? = null,
        cursorColumn: Int? = null,
        selectedCodeRange: String? = null
    ): XeroDeveloperContext
}

class XeroContextProviderImpl(
    private val workspaceManager: WorkspaceManager?,
    private val diagnosticsEngine: DiagnosticsEngine?,
    private val buildService: BuildService?,
    private val approvalManager: ActionApprovalManager?,
    private val fileChangeTracker: FileChangeTracker?,
    private val projectIndexer: ProjectIndexerImpl?,
    private val conversationHistory: ConversationHistory?
) : XeroContextProvider {

    override suspend fun gatherDeveloperContext(
        projectPath: String,
        activeFile: String?,
        cursorLine: Int?,
        cursorColumn: Int?,
        selectedCodeRange: String?
    ): XeroDeveloperContext {
        val session = workspaceManager?.getActiveWorkspace()
        val finalActiveFile = activeFile ?: session?.activeFile

        // Gather local diagnostics
        val diagnostics = diagnosticsEngine?.activeDiagnostics?.value ?: emptyList()

        // Gather build results
        val buildResult = buildService?.activeBuildResult?.value

        // Extract and limit changes
        val changes = mutableListOf<String>()
        fileChangeTracker?.let { tracker ->
            changes.addAll(tracker.createdFiles.map { "Created: $it" })
            changes.addAll(tracker.modifiedFiles.map { "Modified: $it" })
            changes.addAll(tracker.deletedFiles.map { "Deleted: $it" })
        }
        val limitedChanges = changes.distinct().take(10)

        // Gather relevant project symbols
        val symbols = mutableListOf<String>()
        if (projectIndexer != null && projectPath.isNotEmpty()) {
            projectIndexer.getIndex(projectPath)?.let { index ->
                symbols.addAll(index.symbols.map { "${it.type} ${it.name}" }.take(15))
            }
        }

        // Fetch local conversation entries
        val conversationEntries = conversationHistory?.getHistory() ?: emptyList()

        // Fetch pending action approvals
        val pendingActions = approvalManager?.getPendingActions() ?: emptyList()

        // Sanitize any potential raw file path or content range inputs
        val sanitizedActiveFile = finalActiveFile?.let { sanitizeText(it) }
        val sanitizedSelectedRange = selectedCodeRange?.let { sanitizeText(it) }

        return XeroDeveloperContext(
            workspaceSession = session,
            activeFile = sanitizedActiveFile,
            cursorLine = cursorLine,
            cursorColumn = cursorColumn,
            selectedCodeRange = sanitizedSelectedRange,
            recentFileChanges = limitedChanges.map { sanitizeText(it) },
            currentDiagnostics = diagnostics,
            latestBuildResult = buildResult,
            relevantSymbols = symbols.map { sanitizeText(it) },
            recentConversationHistory = conversationEntries,
            pendingAiActions = pendingActions
        )
    }

    private fun sanitizeText(input: String): String {
        var sanitized = input
        sanitized = sanitized.replace(Regex("(?i)(api_?key|secret|password|token)[\\s:=]+[a-zA-Z0-9_\\-'\"]+"), "$1=REDACTED")
        sanitized = sanitized.replace(Regex("AIzaSy[A-Za-z0-9_\\-]{33}"), "AIzaSy_REDACTED")
        sanitized = sanitized.replace(Regex("(?i)(keystore|private_key)"), "REDACTED_SENSITIVE")
        return sanitized
    }
}
