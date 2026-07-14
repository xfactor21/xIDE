package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.provider.XideProvider
import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.ai.AiContextManager
import java.io.File

interface CodeIntelligenceEngine : XideProvider {
    suspend fun analyzeSourceFile(projectPath: String, filePath: String): CodeSummary
    suspend fun getSymbolRelationships(projectPath: String): List<ProjectRelationship>
    suspend fun findSymbolReferences(projectPath: String, symbolName: String): List<ProjectSymbol>
}

data class CodeSummary(
    val filePath: String,
    val className: String?,
    val symbols: List<ProjectSymbol>,
    val imports: List<String>,
    val linesCount: Int
)

class CodeIntelligenceEngineImpl(
    private val projectIndexer: ProjectIndexer,
    private val vfs: VirtualFileSystem,
    private val diagnosticsEngine: DiagnosticsEngine?,
    private val aiContextManager: AiContextManager?
) : CodeIntelligenceEngine {

    override val providerId: String = "code-intelligence-engine-default"
    override val providerName: String = "Default Code Intelligence Engine"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("symbol-analysis", "relationship-tracking", "references-search")
    override val requirements: List<String> = listOf("ProjectIndexer", "VirtualFileSystem")
    override val limitations: List<String> = emptyList()
    override val description: String = "Provides offline analysis of codebase symbols and dependencies"
    override val author: String = "Principal Architect"
    override val compatibilityVersion: String = "1.0.0"

    override suspend fun initialize() {}
    override suspend fun shutdown() {}
    override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY

    override suspend fun analyzeSourceFile(projectPath: String, filePath: String): CodeSummary {
        val safePath = vfs.validatePath(filePath)
        val content = vfs.openFile(safePath)
        val fileLines = content.lines()
        
        val index = projectIndexer.getIndex(projectPath)
        val fileSymbols = index?.symbols?.filter { 
            it.location.startsWith(safePath) || it.location.substringBefore(":").endsWith(File(safePath).name)
        } ?: emptyList()

        val imports = fileLines.filter { it.trim().startsWith("import ") }
            .map { it.trim().substringAfter("import ").trim() }

        val className = fileSymbols.firstOrNull { it.type == "Class" }?.name

        return CodeSummary(
            filePath = safePath,
            className = className,
            symbols = fileSymbols,
            imports = imports,
            linesCount = fileLines.size
        )
    }

    override suspend fun getSymbolRelationships(projectPath: String): List<ProjectRelationship> {
        val index = projectIndexer.getIndex(projectPath)
        return index?.relationships ?: emptyList()
    }

    override suspend fun findSymbolReferences(projectPath: String, symbolName: String): List<ProjectSymbol> {
        val index = projectIndexer.getIndex(projectPath) ?: return emptyList()
        // References can be found where symbols are used/imported or references are listed
        return index.symbols.filter { symbol ->
            symbol.name == symbolName || symbol.references.contains(symbolName)
        }
    }
}
