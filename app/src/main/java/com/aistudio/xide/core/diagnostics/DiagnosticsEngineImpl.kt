package com.aistudio.xide.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiagnosticsEngineImpl : DiagnosticsEngine {
    private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
    override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = _activeDiagnostics.asStateFlow()

    override fun addDiagnostic(diagnostic: BuildDiagnostic) {
        _activeDiagnostics.value = _activeDiagnostics.value + sanitizeDiagnostic(diagnostic)
    }

    override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {
        _activeDiagnostics.value = _activeDiagnostics.value + diagnostics.map { sanitizeDiagnostic(it) }
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

    private fun sanitizeDiagnostic(diag: BuildDiagnostic): BuildDiagnostic {
        val cleanMsg = sanitizeText(diag.message)
        val cleanRaw = diag.rawOutput?.let { sanitizeText(it) }
        val cleanCompDiag = diag.compilerDiagnostic?.let { comp ->
            comp.copy(
                message = sanitizeText(comp.message)
            )
        }
        return diag.copy(
            message = cleanMsg,
            compilerDiagnostic = cleanCompDiag,
            rawOutput = cleanRaw
        )
    }

    private fun sanitizeText(text: String): String {
        // 1. Remove ANSI escape characters
        val noAnsi = text.replace(Regex("\u001B\\[[;\\d]*[a-zA-Z]"), "")
        
        // 2. Redact potential API keys or passwords
        val credentialRegex = Regex("""(?i)(api_key|secret|password|token|credential|auth)[=:\s"']+[A-Za-z0-9_\-]{16,}""")
        val redactedCreds = noAnsi.replace(credentialRegex) { matchResult ->
            val key = matchResult.groupValues[1]
            "$key=[REDACTED_SECRET]"
        }

        // 3. Limit message size to prevent AI context bloating
        return if (redactedCreds.length > 1000) {
            redactedCreds.substring(0, 1000) + "... [TRUNCATED]"
        } else {
            redactedCreds
        }
    }
}
