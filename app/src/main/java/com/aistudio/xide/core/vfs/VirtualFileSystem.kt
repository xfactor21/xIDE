package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.events.EventBus
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class VirtualFileSystem(
    private val fsProvider: FileSystemProvider,
    private val eventBus: EventBus,
    private val indexer: ProjectIndexerImpl
) {
    private var activeWorkspaceRoot: String? = null

    fun setActiveWorkspaceRoot(rootPath: String) {
        activeWorkspaceRoot = rootPath
    }

    fun getActiveWorkspaceRoot(): String? = activeWorkspaceRoot

    /**
     * Secures and validates that the requested path is safely located inside the active workspace.
     * Prevents path-traversal attacks.
     */
    fun validatePath(path: String): String {
        val root = activeWorkspaceRoot ?: throw IllegalStateException("No active project workspace open")
        val canonicalRoot = File(root).canonicalPath
        val canonicalTarget = File(path).canonicalPath
        if (!canonicalTarget.startsWith(canonicalRoot)) {
            throw SecurityException("Access denied: Path lies outside workspace root: $path")
        }
        return canonicalTarget
    }

    suspend fun listFiles(path: String): List<FileNode> {
        val safePath = validatePath(path)
        return fsProvider.list(safePath)
    }

    suspend fun openFile(path: String): String {
        val safePath = validatePath(path)
        return fsProvider.readFile(safePath).bufferedReader().use { it.readText() }
    }

    suspend fun createFile(path: String, content: String): Boolean {
        val safePath = validatePath(path)
        if (fsProvider.exists(safePath)) {
            return false
        }
        updateFileContent(safePath, content)
        eventBus.publish(VfsEvent.FileCreated(safePath))
        activeWorkspaceRoot?.let { indexer.indexProject(it) }
        return true
    }

    suspend fun updateFile(path: String, content: String): Boolean {
        val safePath = validatePath(path)
        if (!fsProvider.exists(safePath)) {
            return false
        }
        updateFileContent(safePath, content)
        eventBus.publish(com.aistudio.xide.core.events.FileChanged(
            eventId = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            filePath = safePath
        ))
        activeWorkspaceRoot?.let { indexer.indexProject(it) }
        return true
    }

    suspend fun deleteFile(path: String): Boolean {
        val safePath = validatePath(path)
        if (!fsProvider.exists(safePath)) {
            return false
        }
        val deleted = fsProvider.delete(safePath)
        if (deleted) {
            eventBus.publish(VfsEvent.FileDeleted(safePath))
            activeWorkspaceRoot?.let { indexer.indexProject(it) }
        }
        return deleted
    }

    suspend fun moveFile(sourcePath: String, targetPath: String): Boolean {
        val safeSource = validatePath(sourcePath)
        val safeTarget = validatePath(targetPath)
        if (!fsProvider.exists(safeSource)) {
            return false
        }
        val moved = fsProvider.move(safeSource, safeTarget)
        if (moved) {
            eventBus.publish(VfsEvent.FileDeleted(safeSource))
            eventBus.publish(VfsEvent.FileCreated(safeTarget))
            activeWorkspaceRoot?.let { indexer.indexProject(it) }
        }
        return moved
    }

    suspend fun searchFiles(query: String): List<FileNode> {
        val root = activeWorkspaceRoot ?: return emptyList()
        val results = mutableListOf<FileNode>()
        searchRecursively(root, query, results)
        return results
    }

    private suspend fun searchRecursively(dirPath: String, query: String, results: MutableList<FileNode>) {
        val nodes = fsProvider.list(dirPath)
        for (node in nodes) {
            if (node.name.contains(query, ignoreCase = true)) {
                results.add(node)
            }
            if (node.isDirectory) {
                if (node.name !in listOf(".git", ".gradle", "build", "node_modules")) {
                    searchRecursively(node.path, query, results)
                }
            }
        }
    }

    private suspend fun updateFileContent(path: String, content: String) {
        fsProvider.writeFile(path).use { output ->
            output.write(content.toByteArray())
            output.flush()
        }
    }
}
