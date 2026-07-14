package com.aistudio.xide.core.workspace

interface WorkspaceHost {
    val container: WorkspaceContainer
    fun loadWorkspace(workspaceId: String)
    fun closeWorkspace()
}
