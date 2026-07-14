package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.diagnostics.DiagnosticAnalyzer
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.diagnostics.BuildDiagnostic

data class DiagnosticExplanation(
    val explanationText: String,
    val affectedFiles: List<String>,
    val rootCauses: List<String>,
    val proposedManualFixes: List<String>
)

interface DiagnosticConversationService {
    fun explainBuildFailure(): DiagnosticExplanation
}

class DiagnosticConversationServiceImpl(
    private val diagnosticsEngine: DiagnosticsEngine,
    private val diagnosticAnalyzer: DiagnosticAnalyzer
) : DiagnosticConversationService {

    override fun explainBuildFailure(): DiagnosticExplanation {
        val diagnostics = diagnosticsEngine.activeDiagnostics.value
        if (diagnostics.isEmpty()) {
            return DiagnosticExplanation(
                explanationText = "All files compiled successfully. There are no active build errors to explain.",
                affectedFiles = emptyList(),
                rootCauses = emptyList(),
                proposedManualFixes = emptyList()
            )
        }

        val groups = diagnosticAnalyzer.analyzeDiagnostics()
        if (groups.isEmpty()) {
            return DiagnosticExplanation(
                explanationText = "An unknown compiler issue occurred. No root causes could be automatically mapped.",
                affectedFiles = emptyList(),
                rootCauses = listOf("General compiler failure"),
                proposedManualFixes = listOf("Review raw build outputs inside the console logs")
            )
        }

        val sb = StringBuilder()
        val allFiles = mutableListOf<String>()
        val allRootCauses = mutableListOf<String>()
        val allFixes = mutableListOf<String>()

        sb.append("Build failed with ${diagnostics.size} compiler diagnostic(s):\n\n")

        for ((index, group) in groups.withIndex()) {
            sb.append("${index + 1}. **Primary Error:** ${group.primaryError}\n")
            
            // Format affected files
            if (group.affectedFiles.isNotEmpty()) {
                val fileDetails = group.affectedFiles.joinToString(", ") { path ->
                    path.substringAfterLast('/')
                }
                sb.append("   - **Location:** Referenced in $fileDetails\n")
                allFiles.addAll(group.affectedFiles)
            }

            // Group causes and proposed manual fixes
            if (group.rootCauseCandidates.isNotEmpty()) {
                sb.append("   - **Possible Root Causes:**\n")
                group.rootCauseCandidates.forEach { cause ->
                    sb.append("     * $cause\n")
                    allRootCauses.add(cause)
                }
            }

            if (group.suggestedActions.isNotEmpty()) {
                sb.append("   - **Suggested Manual Fixes:**\n")
                group.suggestedActions.forEach { action ->
                    sb.append("     * $action\n")
                    allFixes.add(action)
                }
            }
            sb.append("\n")
        }

        return DiagnosticExplanation(
            explanationText = sb.toString().trim(),
            affectedFiles = allFiles.distinct(),
            rootCauses = allRootCauses.distinct(),
            proposedManualFixes = allFixes.distinct()
        )
    }
}
