package com.aistudio.xide.core.diagnostics

/**
 * Represents a detailed compiler diagnostic, such as syntax errors or warnings.
 */
data class CompilerDiagnostic(
    val code: String?, // e.g. "UNRESOLVED_REFERENCE"
    val message: String,
    val severity: DiagnosticSeverity,
    val location: DiagnosticLocation?
)
