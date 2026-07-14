package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildState
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.workspace.WorkspaceManager
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.intelligence.CodeNavigator
import java.io.File

data class SelectedContext(
    val activeFile: String?,
    val activeFileSnippet: String?,
    val relatedSymbols: List<String>,
    val diagnostics: List<String>,
    val dependencyInformation: List<String>,
    val recentChanges: List<String>,
    val buildStatus: String,
    val contextKeys: List<String>
)

class ContextSelector(
    private val workspaceManager: WorkspaceManager?,
    private val projectIndexer: ProjectIndexerImpl?,
    private val codeNavigator: CodeNavigator?,
    private val diagnosticsEngine: DiagnosticsEngine?,
    private val fileChangeTracker: FileChangeTracker?,
    private val buildService: BuildService?,
    private val vfs: VirtualFileSystem?
) {

    suspend fun selectContext(
        intent: DeveloperIntent,
        queryText: String,
        projectPath: String,
        activeFile: String? = null
    ): SelectedContext {
        val finalActiveFile = activeFile ?: workspaceManager?.getActiveWorkspace()?.activeFile
        
        val snippet = if (finalActiveFile != null) {
            getSafeFileSnippet(finalActiveFile)
        } else {
            null
        }

        val symbols = mutableListOf<String>()
        if (projectIndexer != null && projectPath.isNotEmpty()) {
            val index = projectIndexer.getIndex(projectPath)
            if (index != null) {
                val matchingSymbols = index.symbols.filter { symbol ->
                    queryText.contains(symbol.name, ignoreCase = true) || 
                    (finalActiveFile != null && symbol.location.contains(finalActiveFile))
                }.map { "${it.type} ${it.name} in ${it.location}" }
                
                symbols.addAll(matchingSymbols.take(10))
            }
        }

        val activeDiagnostics = mutableListOf<String>()
        diagnosticsEngine?.activeDiagnostics?.value?.let { list ->
            val formatted = list.filter { it.severity == com.aistudio.xide.core.diagnostics.DiagnosticSeverity.ERROR }
                .map { "[${it.category}] ${it.message}" }
            activeDiagnostics.addAll(formatted.take(3))
        }

        val dependencies = mutableListOf<String>()
        if (codeNavigator != null && projectPath.isNotEmpty() && finalActiveFile != null) {
            val fileName = finalActiveFile.substringAfterLast('/')
            val refs = codeNavigator.findReferences(projectPath, fileName.substringBeforeLast('.'))
            dependencies.addAll(refs.map { "Referenced by: $it" }.take(5))
        }

        val recentChanges = mutableListOf<String>()
        fileChangeTracker?.let { tracker ->
            recentChanges.addAll(tracker.createdFiles.map { "Created: $it" })
            recentChanges.addAll(tracker.modifiedFiles.map { "Modified: $it" })
            recentChanges.addAll(tracker.deletedFiles.map { "Deleted: $it" })
        }
        val limitedChanges = recentChanges.distinct().take(5)

        val buildStatus = buildService?.buildState?.value?.name ?: "IDLE"

        val contextKeys = mutableListOf<String>()
        if (finalActiveFile != null) contextKeys.add("active_file")
        if (symbols.isNotEmpty()) contextKeys.add("symbols")
        if (activeDiagnostics.isNotEmpty()) contextKeys.add("diagnostics")
        if (dependencies.isNotEmpty()) contextKeys.add("dependencies")
        if (limitedChanges.isNotEmpty()) contextKeys.add("recent_changes")
        contextKeys.add("build_status")

        return SelectedContext(
            activeFile = finalActiveFile,
            activeFileSnippet = snippet,
            relatedSymbols = symbols,
            diagnostics = activeDiagnostics,
            dependencyInformation = dependencies,
            recentChanges = limitedChanges,
            buildStatus = buildStatus,
            contextKeys = contextKeys
        )
    }

    private suspend fun getSafeFileSnippet(path: String): String {
        val fileName = path.substringAfterLast('/')
        
        if (fileName.endsWith(".env") || 
            fileName.contains("secret", ignoreCase = true) || 
            fileName.equals("local.properties", ignoreCase = true) || 
            fileName.equals("keystore", ignoreCase = true)) {
            return "[SECURITY BLOCK: File content hidden due to high-risk secret patterns]"
        }

        if (vfs == null) return "[Snippet unavailable: VFS not configured]"

        return try {
            val content = vfs.openFile(path)
            val lines = content.lines()
            val truncatedLines = lines.take(30)
            val joined = truncatedLines.joinToString("\n") { line ->
                sanitizeLine(line)
            }
            
            if (lines.size > 30) {
                "$joined\n... [Truncated to 30 lines to prevent excessive token usage]"
            } else {
                joined
            }
        } catch (e: Exception) {
            "[Error reading file snippet: ${e.message}]"
        }
    }

    private fun sanitizeLine(line: String): String {
        var sanitized = line
        sanitized = sanitized.replace(Regex("(?i)(api_?key|secret|password|token)[\\s:=]+[a-zA-Z0-9_\\-\"\']+"), "$1=REDACTED")
        sanitized = sanitized.replace(Regex("AIzaSy[A-Za-z0-9_\\-]{33}"), "AIzaSy_REDACTED")
        return sanitized
    }
}
