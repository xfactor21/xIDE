package com.aistudio.xide.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiagnosticsEngineImpl : DiagnosticsEngine {
    private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
    override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = _activeDiagnostics.asStateFlow()

    override fun addDiagnostic(diagnostic: BuildDiagnostic) {
        _activeDiagnostics.value = _activeDiagnostics.value + diagnostic
    }

    override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {
        _activeDiagnostics.value = _activeDiagnostics.value + diagnostics
    }

    override fun clearDiagnostics() {
        _activeDiagnostics.value = emptyList()
    }

    override fun getFormattedDiagnosticsForContext(): List<String> {
        return _activeDiagnostics.value.map { diag ->
            val severityStr = diag.severity.name
            val locationStr = diag.compilerDiagnostic?.location?.let { loc ->
                val file = loc.filePath ?: "unknown file"
                val lineStr = loc.line?.let { " line $it" } ?: ""
                val colStr = loc.column?.let { " col $it" } ?: ""
                "at $file$lineStr$colStr"
            } ?: ""
            "[$severityStr] ${diag.message} $locationStr".trim()
        }
    }
}
