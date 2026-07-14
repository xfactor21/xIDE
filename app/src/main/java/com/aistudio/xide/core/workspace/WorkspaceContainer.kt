package com.aistudio.xide.core.workspace

import com.aistudio.xide.core.provider.WorkspaceProvider

interface WorkspaceContainer {
    val activeWorkspaceProvider: WorkspaceProvider?
    fun attachWorkspace(provider: WorkspaceProvider)
    fun detachWorkspace()
}
