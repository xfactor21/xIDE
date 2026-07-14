package com.aistudio.xide.core.provider

import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction for virtual file systems.
 */
interface FileSystemProvider : XideProvider {
    val protocol: String // e.g., "local", "saf", "git", "cloud"
    
    suspend fun list(path: String): List<FileNode>
    suspend fun readFile(path: String): InputStream
    suspend fun writeFile(path: String): OutputStream
    suspend fun delete(path: String): Boolean
    suspend fun mkdir(path: String): Boolean
    suspend fun exists(path: String): Boolean
    suspend fun move(sourcePath: String, targetPath: String): Boolean {
        // Default naive implementation, should be overridden by concrete providers
        if (!exists(sourcePath)) return false
        val content = readFile(sourcePath).use { it.readBytes() }
        writeFile(targetPath).use { it.write(content) }
        delete(sourcePath)
        return true
    }
}

data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long
)
