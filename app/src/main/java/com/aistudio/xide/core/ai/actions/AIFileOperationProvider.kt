package com.aistudio.xide.core.ai.actions

import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import java.io.FileNotFoundException

data class RollbackInfo(
    val actionId: String,
    val filePath: String,
    val operationType: String, // "CREATE", "UPDATE", "DELETE", "RENAME"
    val previousContent: String?,
    val previousPath: String? = null
)

data class OperationResult(
    val success: Boolean,
    val changedFiles: List<String>,
    val diagnostics: List<String>,
    val rollbackInfo: RollbackInfo?,
    val errorMessage: String? = null
)

class AIFileOperationProvider(
    private val vfs: VirtualFileSystem,
    private val diagnosticsEngine: DiagnosticsEngine? = null
) {
    suspend fun executeAction(action: AIAction): OperationResult {
        return when (action) {
            is AIAction.CreateFile -> {
                try {
                    val safePath = vfs.validatePath(action.path)
                    vfs.createFile(safePath, action.content)
                    OperationResult(
                        success = true,
                        changedFiles = listOf(safePath),
                        diagnostics = diagnosticsEngine?.getFormattedDiagnosticsForContext() ?: emptyList(),
                        rollbackInfo = RollbackInfo(action.actionId, safePath, "CREATE", null)
                    )
                } catch (e: Exception) {
                    OperationResult(
                        success = false,
                        changedFiles = emptyList(),
                        diagnostics = emptyList(),
                        rollbackInfo = null,
                        errorMessage = e.message
                    )
                }
            }
            is AIAction.ModifyFile -> {
                try {
                    val safePath = vfs.validatePath(action.path)
                    val originalContent = try {
                        vfs.openFile(safePath)
                    } catch (e: Exception) {
                        ""
                    }
                    vfs.updateFile(safePath, action.proposedContent)
                    OperationResult(
                        success = true,
                        changedFiles = listOf(safePath),
                        diagnostics = diagnosticsEngine?.getFormattedDiagnosticsForContext() ?: emptyList(),
                        rollbackInfo = RollbackInfo(action.actionId, safePath, "UPDATE", originalContent)
                    )
                } catch (e: Exception) {
                    OperationResult(
                        success = false,
                        changedFiles = emptyList(),
                        diagnostics = emptyList(),
                        rollbackInfo = null,
                        errorMessage = e.message
                    )
                }
            }
            is AIAction.DeleteFile -> {
                try {
                    val safePath = vfs.validatePath(action.path)
                    val originalContent = try {
                        vfs.openFile(safePath)
                    } catch (e: Exception) {
                        null
                    }
                    vfs.deleteFile(safePath)
                    OperationResult(
                        success = true,
                        changedFiles = listOf(safePath),
                        diagnostics = diagnosticsEngine?.getFormattedDiagnosticsForContext() ?: emptyList(),
                        rollbackInfo = RollbackInfo(action.actionId, safePath, "DELETE", originalContent)
                    )
                } catch (e: Exception) {
                    OperationResult(
                        success = false,
                        changedFiles = emptyList(),
                        diagnostics = emptyList(),
                        rollbackInfo = null,
                        errorMessage = e.message
                    )
                }
            }
            is AIAction.RenameFile -> {
                try {
                    val safeOldPath = vfs.validatePath(action.oldPath)
                    val safeNewPath = vfs.validatePath(action.newPath)
                    val content = vfs.openFile(safeOldPath)
                    
                    vfs.deleteFile(safeOldPath)
                    vfs.createFile(safeNewPath, content)
                    
                    OperationResult(
                        success = true,
                        changedFiles = listOf(safeOldPath, safeNewPath),
                        diagnostics = diagnosticsEngine?.getFormattedDiagnosticsForContext() ?: emptyList(),
                        rollbackInfo = RollbackInfo(action.actionId, safeNewPath, "RENAME", content, safeOldPath)
                    )
                } catch (e: Exception) {
                    OperationResult(
                        success = false,
                        changedFiles = emptyList(),
                        diagnostics = emptyList(),
                        rollbackInfo = null,
                        errorMessage = e.message
                    )
                }
            }
            else -> {
                OperationResult(
                    success = true,
                    changedFiles = emptyList(),
                    diagnostics = emptyList(),
                    rollbackInfo = null
                )
            }
        }
    }
}
