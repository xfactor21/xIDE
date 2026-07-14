package com.aistudio.xide.core.intelligence.analysis

import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.intelligence.ProjectSymbol
import com.aistudio.xide.core.provider.XideProvider

/**
 * Provides static code analysis capabilities for different languages.
 */
interface CodeAnalysisProvider : XideProvider {
    val supportedLanguages: List<LanguageDefinition>
    
    suspend fun analyzeFile(filePath: String, content: String): FileAnalysisResult
    suspend fun discoverSymbols(filePath: String, content: String): List<ProjectSymbol>
    suspend fun getProjectContext(projectPath: String): ProjectContext
}

data class LanguageDefinition(
    val languageId: String,
    val displayName: String,
    val fileExtensions: List<String>,
    val capabilities: List<String>
)

data class FileAnalysisResult(
    val filePath: String,
    val languageId: String,
    val metadata: Map<String, String>,
    val symbols: List<ProjectSymbol>,
    val issues: List<String> // Will be connected to Diagnostics later
)
