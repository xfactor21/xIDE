package com.aistudio.xide.core.workspace

interface WorkspaceLifecycle {
    fun onWorkspaceCreated(projectId: String)
    fun onWorkspaceOpened(projectId: String)
    fun onWorkspaceClosed(projectId: String)
}

interface WorkspacePersistence {
    suspend fun saveState(session: WorkspaceSession)
    suspend fun loadState(projectId: String): WorkspaceSession?
}

interface WorkspaceCache {
    fun put(key: String, value: Any)
    fun get(key: String): Any?
    fun clear()
}

interface WorkspaceRestore {
    suspend fun restoreSession(projectId: String): Boolean
}

data class WorkspaceMetadata(
    val projectId: String,
    val properties: Map<String, String>
)
