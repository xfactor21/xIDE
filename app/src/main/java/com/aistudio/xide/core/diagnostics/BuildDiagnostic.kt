package com.aistudio.xide.core.diagnostics

/**
 * Represents a diagnostic outcome from the build process.
 */
data class BuildDiagnostic(
    val category: String, // e.g. "compiler", "dependency", "environment", "configuration"
    val severity: DiagnosticSeverity,
    val message: String,
    val compilerDiagnostic: CompilerDiagnostic? = null,
    val rawOutput: String? = null
)
