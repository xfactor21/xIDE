package com.aistudio.xide.core.diagnostics

import com.aistudio.xide.core.provider.XideProvider
import kotlinx.coroutines.flow.Flow

interface DiagnosticProvider : XideProvider {
    val source: DiagnosticSource
    fun observeDiagnostics(filePath: String): Flow<List<DiagnosticItem>>
    suspend fun requestDiagnostics(filePath: String): List<DiagnosticItem>
}

enum class DiagnosticSource {
    COMPILER, STATIC_ANALYSIS, LINT, SECURITY, AI_SUGGESTION
}

enum class DiagnosticSeverity {
    ERROR, WARNING, INFO, HINT
}

data class DiagnosticItem(
    val id: String,
    val filePath: String,
    val severity: DiagnosticSeverity,
    val source: DiagnosticSource,
    val message: String,
    val line: Int,
    val column: Int,
    val suggestedFixes: List<String> = emptyList()
)
