package com.aistudio.xide.core.provider

/**
 * Abstraction for Cloud capabilities (Sync, Storage, etc.).
 */
interface CloudProvider : XideProvider {
    suspend fun syncProject(projectPath: String)
    suspend fun uploadFile(localPath: String, remotePath: String)
    suspend fun downloadFile(remotePath: String, localPath: String)
}
