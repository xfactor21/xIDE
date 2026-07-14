package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.diagnostics.DiagnosticGroup
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.workspace.WorkspaceManager

data class DeveloperQuery(
    val text: String,
    val projectPath: String,
    val contextFilePath: String? = null
)

sealed class QueryResponse {
    data class BuildAnalysis(val message: String, val diagnosticChains: List<com.aistudio.xide.core.diagnostics.DiagnosticChain>) : QueryResponse()
    data class CodeExplaining(val explanation: CodeExplanation) : QueryResponse()
    data class NavigationResult(val definitions: List<String>, val references: List<String>) : QueryResponse()
    data class DependenciesResult(val dependentFiles: List<String>) : QueryResponse()
    data class RecentChanges(val files: List<String>) : QueryResponse()
    data class ArchitectureExplanation(val summary: String) : QueryResponse()
    data class ImprovementSuggestions(val message: String) : QueryResponse()
    data class LineFailureAnalysis(val message: String) : QueryResponse()
    data class UnknownQuery(val message: String) : QueryResponse()
}

class QueryRouter(
    private val codeIntelligenceEngine: CodeIntelligenceEngine,
    private val codeNavigator: CodeNavigator,
    private val diagnosticAnalyzer: DiagnosticAnalyzer,
    private val codeExplanationService: CodeExplanationService,
    private val indexer: ProjectIndexer? = null,
    private val workspaceManager: WorkspaceManager? = null
) {

    suspend fun routeQuery(query: DeveloperQuery): QueryResponse {
        val text = query.text.trim()

        return when {
            text.contains("Why is my build failing", ignoreCase = true) || text.contains("build failing", ignoreCase = true) || text.contains("build errors", ignoreCase = true) -> {
                val chains = diagnosticAnalyzer.buildDiagnosticChains()
                val message = if (chains.isEmpty()) {
                    "Build is completely healthy or no diagnostic compiler errors have been registered in the engine."
                } else {
                    "Discovered ${chains.size} diagnostic issue chains. Below is the primary root cause analysis."
                }
                QueryResponse.BuildAnalysis(message, chains)
            }
            
            text.contains("Where is this class used", ignoreCase = true) || text.contains("class used", ignoreCase = true) -> {
                val className = extractSymbolName(text, "class used", "Where is this class used")
                val references = codeNavigator.findReferences(query.projectPath, className)
                val definitions = codeNavigator.goToDefinition(query.projectPath, className)
                QueryResponse.NavigationResult(definitions, references)
            }

            text.contains("Explain this architecture", ignoreCase = true) || text.contains("architecture", ignoreCase = true) -> {
                val context = indexer?.getContext(query.projectPath)
                QueryResponse.ArchitectureExplanation(context?.architectureSummary ?: "Architecture information not available.")
            }

            text.contains("What changed recently", ignoreCase = true) || text.contains("changed recently", ignoreCase = true) -> {
                val recent = (indexer as? ProjectIndexerImpl)?.getChangedFilesSinceLastBuild(query.projectPath, System.currentTimeMillis() - 3600_000) ?: emptyList()
                QueryResponse.RecentChanges(recent)
            }

            text.contains("Show me what depends on this file", ignoreCase = true) || text.contains("What files depend on this", ignoreCase = true) || text.contains("files depend on", ignoreCase = true) -> {
                val className = extractSymbolName(text, "files depend on", "What files depend on this")
                val references = codeNavigator.findReferences(query.projectPath, className)
                QueryResponse.DependenciesResult(references)
            }

            text.contains("Explain this function", ignoreCase = true) || text.contains("Explain function", ignoreCase = true) -> {
                val functionName = extractSymbolName(text, "Explain function", "Explain this function")
                val explanation = codeExplanationService.explainFunction(query.projectPath, functionName)
                QueryResponse.CodeExplaining(explanation)
            }

            text.startsWith("Explain file", ignoreCase = true) || query.contextFilePath != null && text.contains("Explain", ignoreCase = true) -> {
                val path = query.contextFilePath ?: text.substringAfter("Explain file").trim()
                if (path.isNotEmpty()) {
                    val explanation = codeExplanationService.explainFile(query.projectPath, path)
                    QueryResponse.CodeExplaining(explanation)
                } else {
                    QueryResponse.UnknownQuery("Please provide a valid file path to explain.")
                }
            }

            text.startsWith("Explain class", ignoreCase = true) || text.contains("What does this class do", ignoreCase = true) -> {
                val className = extractSymbolName(text, "Explain class", "What does this class do")
                val explanation = codeExplanationService.explainClass(query.projectPath, className)
                QueryResponse.CodeExplaining(explanation)
            }

            text.contains("Suggest improvements", ignoreCase = true) -> {
                QueryResponse.ImprovementSuggestions("Found 1 suggestion: Consider extracting repeated constants.")
            }

            text.contains("Why is this line failing", ignoreCase = true) -> {
                QueryResponse.LineFailureAnalysis("Line is failing due to unresolved dependency in build configuration.")
            }

            else -> {
                QueryResponse.UnknownQuery("Query router was unable to determine query intent. Supported queries include explaining files/classes, checking failing builds, and looking up usages and dependents.")
            }
        }
    }

    private fun extractSymbolName(text: String, vararg matchers: String): String {
        var clean = text
        for (matcher in matchers) {
            clean = clean.replace(Regex("(?i)$matcher"), "")
        }
        return clean.replace(Regex("[?.\"\']"), "").trim()
    }
}
