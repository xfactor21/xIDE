package com.aistudio.xide.core.diagnostics

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages active diagnostic logs from compilers and builds, providing formatted contexts for Xero.
 */
interface DiagnosticsEngine {
    val activeDiagnostics: StateFlow<List<BuildDiagnostic>>
    fun addDiagnostic(diagnostic: BuildDiagnostic)
    fun addDiagnostics(diagnostics: List<BuildDiagnostic>)
    fun clearDiagnostics()
    fun getFormattedDiagnosticsForContext(): List<String>
}
