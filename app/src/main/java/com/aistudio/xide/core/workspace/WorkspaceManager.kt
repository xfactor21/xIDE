package com.aistudio.xide.core.workspace

import com.aistudio.xide.core.provider.XideProvider

interface WorkspaceManager : XideProvider {
    fun createProject(name: String, path: String)
    fun openProject(projectId: String)
    fun closeCurrentProject()
    fun getActiveWorkspace(): WorkspaceSession?
}
