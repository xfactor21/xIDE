package com.aistudio.xide.core.diagnostics

/**
 * Represents the exact source code location associated with a diagnostic.
 */
data class DiagnosticLocation(
    val filePath: String?,
    val line: Int?,
    val column: Int?
)
