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
}

data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long
)
