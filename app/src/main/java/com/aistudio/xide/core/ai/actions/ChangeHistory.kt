package com.aistudio.xide.core.ai.actions

import com.aistudio.xide.core.vfs.VirtualFileSystem
import java.util.concurrent.ConcurrentLinkedDeque

data class HistoryEntry(
    val actionId: String,
    val filePath: String,
    val operationType: String,
    val previousContent: String?,
    val previousPath: String?,
    val previousContentHash: String,
    val timestamp: Long
)

class ChangeHistory(
    private val vfs: VirtualFileSystem
) {
    private val history = ConcurrentLinkedDeque<HistoryEntry>()

    fun recordChange(rollbackInfo: RollbackInfo) {
        val hash = rollbackInfo.previousContent?.let { sha256(it) } ?: ""
        val entry = HistoryEntry(
            actionId = rollbackInfo.actionId,
            filePath = rollbackInfo.filePath,
            operationType = rollbackInfo.operationType,
            previousContent = rollbackInfo.previousContent,
            previousPath = rollbackInfo.previousPath,
            previousContentHash = hash,
            timestamp = System.currentTimeMillis()
        )
        history.addLast(entry)
    }

    suspend fun rollbackLastChange(): Boolean {
        val entry = history.pollLast() ?: return false
        return try {
            when (entry.operationType) {
                "CREATE" -> {
                    val safePath = vfs.validatePath(entry.filePath)
                    vfs.deleteFile(safePath)
                }
                "UPDATE" -> {
                    val safePath = vfs.validatePath(entry.filePath)
                    vfs.updateFile(safePath, entry.previousContent ?: "")
                }
                "DELETE" -> {
                    val safePath = vfs.validatePath(entry.filePath)
                    vfs.createFile(safePath, entry.previousContent ?: "")
                }
                "RENAME" -> {
                    val safeNewPath = vfs.validatePath(entry.filePath)
                    val safeOldPath = vfs.validatePath(entry.previousPath ?: throw IllegalStateException("No previous path for rename rollback"))
                    
                    val content = vfs.openFile(safeNewPath)
                    vfs.deleteFile(safeNewPath)
                    vfs.createFile(safeOldPath, content)
                }
            }
            true
        } catch (e: Exception) {
            history.addLast(entry)
            false
        }
    }

    fun getHistory(): List<HistoryEntry> {
        return history.toList()
    }

    fun clear() {
        history.clear()
    }

    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
