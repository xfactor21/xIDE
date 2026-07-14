package com.aistudio.xide.core.workspace

data class WorkspaceSession(
    val sessionId: String,
    val projectId: String,
    val openFiles: List<String>,
    val activeFile: String?
)
