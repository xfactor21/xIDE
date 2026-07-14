package com.aistudio.xide.core.ai.actions

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

enum class ApprovalRequirement {
    EXPLICIT_CONFIRMATION, AUTO_APPROVED, READ_ONLY
}

sealed class AIAction {
    abstract val actionId: String
    abstract val timestamp: Long
    abstract val source: String
    abstract val targetFiles: List<String>
    abstract val description: String
    abstract val riskLevel: RiskLevel
    abstract val approvalRequirement: ApprovalRequirement

    data class CreateFile(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val path: String,
        val content: String,
        override val description: String = "Create file at $path",
        override val riskLevel: RiskLevel = RiskLevel.MEDIUM,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.EXPLICIT_CONFIRMATION
    ) : AIAction() {
        override val targetFiles: List<String> = listOf(path)
    }

    data class ModifyFile(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val path: String,
        val proposedContent: String,
        override val description: String = "Modify file at $path",
        override val riskLevel: RiskLevel = RiskLevel.HIGH,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.EXPLICIT_CONFIRMATION
    ) : AIAction() {
        override val targetFiles: List<String> = listOf(path)
    }

    data class DeleteFile(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val path: String,
        override val description: String = "Delete file at $path",
        override val riskLevel: RiskLevel = RiskLevel.HIGH,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.EXPLICIT_CONFIRMATION
    ) : AIAction() {
        override val targetFiles: List<String> = listOf(path)
    }

    data class RenameFile(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val oldPath: String,
        val newPath: String,
        override val description: String = "Rename file from $oldPath to $newPath",
        override val riskLevel: RiskLevel = RiskLevel.MEDIUM,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.EXPLICIT_CONFIRMATION
    ) : AIAction() {
        override val targetFiles: List<String> = listOf(oldPath, newPath)
    }

    data class ExplainCode(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val code: String,
        override val targetFiles: List<String> = emptyList(),
        override val description: String = "Explain code segment",
        override val riskLevel: RiskLevel = RiskLevel.LOW,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.READ_ONLY
    ) : AIAction()

    data class AnalyzeError(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val errorLog: String,
        override val targetFiles: List<String> = emptyList(),
        override val description: String = "Analyze compilation or runtime error",
        override val riskLevel: RiskLevel = RiskLevel.LOW,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.READ_ONLY
    ) : AIAction()

    data class SuggestFix(
        override val actionId: String,
        override val timestamp: Long,
        override val source: String,
        val problemDescription: String,
        override val targetFiles: List<String> = emptyList(),
        override val description: String = "Suggest architectural or code fix",
        override val riskLevel: RiskLevel = RiskLevel.LOW,
        override val approvalRequirement: ApprovalRequirement = ApprovalRequirement.READ_ONLY
    ) : AIAction()
}
