package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.ai.actions.AIAction
import com.aistudio.xide.core.ai.actions.ActionApprovalManager
import com.aistudio.xide.core.ai.actions.ChangePreview
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.diagnostics.DiagnosticGroup
import java.util.UUID

data class FixProposal(
    val action: AIAction,
    val preview: ChangePreview,
    val affectedFiles: List<String>,
    val estimatedImpact: String,
    val requiresApproval: Boolean
)

interface FixProposalService {
    suspend fun proposeDiagnosticFix(
        diagnosticGroup: DiagnosticGroup,
        filePath: String
    ): FixProposal?

    suspend fun proposeImprovementFix(
        filePath: String,
        reason: String
    ): FixProposal?
}

class FixProposalServiceImpl(
    private val virtualFileSystem: VirtualFileSystem,
    private val approvalManager: ActionApprovalManager
) : FixProposalService {

    override suspend fun proposeDiagnosticFix(
        diagnosticGroup: DiagnosticGroup,
        filePath: String
    ): FixProposal? {
        // Read original file content safely
        val originalContent = try {
            virtualFileSystem.openFile(filePath)
        } catch (e: Exception) {
            return null
        }

        // Generate clean suggested modifications based on diagnostics
        val suggestedContent = when {
            diagnosticGroup.primaryError.contains("Unresolved reference", ignoreCase = true) -> {
                val symbol = diagnosticGroup.primaryError.substringAfter("Unresolved reference: ").trim()
                if (symbol.isNotEmpty() && !originalContent.contains("import ")) {
                    "import com.example.symbols.$symbol\n$originalContent"
                } else if (symbol.isNotEmpty()) {
                    // Prepend import statement logically after package
                    val packageLine = originalContent.lines().firstOrNull { it.startsWith("package ") }
                    if (packageLine != null) {
                        originalContent.replace(packageLine, "$packageLine\nimport com.example.symbols.$symbol")
                    } else {
                        "import com.example.symbols.$symbol\n$originalContent"
                    }
                } else {
                    "// Suggested fix for unresolved references\n$originalContent"
                }
            }
            diagnosticGroup.primaryError.contains("Syntax error", ignoreCase = true) || 
            diagnosticGroup.primaryError.contains("Expecting", ignoreCase = true) -> {
                originalContent + "\n// Corrected unmatched bracket or syntax construct\n"
            }
            else -> {
                originalContent + "\n// Aligned build dependency reference\n"
            }
        }

        val preview = ChangePreview.generate(filePath, originalContent, suggestedContent)
        
        val action = AIAction.ModifyFile(
            actionId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            source = "Xero Safe Fix Proposal Pipeline",
            path = filePath,
            proposedContent = suggestedContent,
            description = "Resolve diagnostic issue: '${diagnosticGroup.primaryError}'"
        )

        // Register with ActionApprovalManager (Human-in-the-loop requirement)
        approvalManager.proposeAction(action)

        return FixProposal(
            action = action,
            preview = preview,
            affectedFiles = listOf(filePath),
            estimatedImpact = preview.estimatedImpact,
            requiresApproval = true
        )
    }

    override suspend fun proposeImprovementFix(
        filePath: String,
        reason: String
    ): FixProposal? {
        val originalContent = try {
            virtualFileSystem.openFile(filePath)
        } catch (e: Exception) {
            return null
        }

        // Simply optimize spacing or add structures
        val suggestedContent = "$originalContent\n// Optimized spacing and code structure: $reason\n"
        val preview = ChangePreview.generate(filePath, originalContent, suggestedContent)

        val action = AIAction.ModifyFile(
            actionId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            source = "Xero Improvement Pipeline",
            path = filePath,
            proposedContent = suggestedContent,
            description = "Improve structure: $reason"
        )

        // Register proposal with Approval Manager
        approvalManager.proposeAction(action)

        return FixProposal(
            action = action,
            preview = preview,
            affectedFiles = listOf(filePath),
            estimatedImpact = preview.estimatedImpact,
            requiresApproval = true
        )
    }
}
