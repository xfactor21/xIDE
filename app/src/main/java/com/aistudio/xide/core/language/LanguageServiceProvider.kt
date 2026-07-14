package com.aistudio.xide.core.language

import com.aistudio.xide.core.provider.XideProvider

/**
 * Foundation for Language Services (LSP, Tree-sitter, etc.)
 */
interface LanguageServiceProvider : XideProvider {
    val languageId: String
    
    suspend fun prepareSyntaxAnalysis(filePath: String, content: String): SyntaxAnalysis
    suspend fun lookupSymbol(filePath: String, line: Int, column: Int): SymbolInfo?
    suspend fun prepareNavigation(filePath: String, line: Int, column: Int): NavigationTarget?
    suspend fun prepareCompletion(filePath: String, content: String, line: Int, column: Int): List<CompletionSuggestion>
    suspend fun lookupDocumentation(filePath: String, line: Int, column: Int): String?
    suspend fun reportErrors(filePath: String, content: String): List<LanguageError>
}

data class SyntaxAnalysis(val isReady: Boolean, val tokens: List<Any>) // Abstracted for future implementation
data class SymbolInfo(val name: String, val type: String, val declarationRange: String)
data class NavigationTarget(val filePath: String, val line: Int, val column: Int)
data class CompletionSuggestion(val label: String, val insertText: String, val kind: String)
data class LanguageError(val line: Int, val column: Int, val message: String, val severity: String)
