package com.aistudio.xide.core.vfs.local

import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class LocalFileSystemProvider : FileSystemProvider {
    override val protocol: String = "local"
    override val providerId: String = "vfs.local"
    override val providerName: String = "Local File System"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("read", "write", "delete", "mkdir", "list")
    override val requirements: List<String> = emptyList()
    override val limitations: List<String> = listOf("Requires standard java.io.File access")
    override val description: String = "Standard local file system"
    override val author: String = "xIDE"
    override val compatibilityVersion: String = "1.0.0"

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth.HEALTHY
    }

    override suspend fun initialize() {
        // Initialization if needed
    }

    override suspend fun shutdown() {
        // Cleanup if needed
    }

    override suspend fun list(path: String): List<FileNode> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.isDirectory) return@withContext emptyList()
        
        file.listFiles()?.map {
            FileNode(
                path = it.absolutePath,
                name = it.name,
                isDirectory = it.isDirectory,
                lastModified = it.lastModified(),
                size = it.length()
            )
        } ?: emptyList()
    }

    override suspend fun readFile(path: String): InputStream = withContext(Dispatchers.IO) {
        FileInputStream(File(path))
    }

    override suspend fun writeFile(path: String): OutputStream = withContext(Dispatchers.IO) {
        FileOutputStream(File(path))
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    override suspend fun mkdir(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).mkdirs()
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun move(sourcePath: String, targetPath: String): Boolean = withContext(Dispatchers.IO) {
        val src = File(sourcePath)
        val dst = File(targetPath)
        if (!src.exists()) return@withContext false
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }
}
