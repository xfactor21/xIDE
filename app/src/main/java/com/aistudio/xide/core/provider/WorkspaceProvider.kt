package com.aistudio.xide.core.provider

/**
 * Abstraction for Workspace management.
 */
interface WorkspaceProvider : XideProvider {
    val workspaceType: String
    suspend fun install()
    suspend fun uninstall()
    suspend fun openProject(path: String)
}
