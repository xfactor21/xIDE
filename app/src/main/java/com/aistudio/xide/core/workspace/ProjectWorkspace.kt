package com.aistudio.xide.core.workspace

enum class WorkspaceState {
    IDLE, LOADING, INDEXING, BUILDING, ERROR
}

data class ProjectWorkspace(
    val id: String,
    val name: String,
    val rootPath: String,
    val metadata: ProjectMetadata,
    val activeFiles: List<String>,
    val state: WorkspaceState
)

data class ProjectMetadata(
    val projectType: String, // "android", "kotlin", "plugin", etc.
    val buildSystem: String, // "gradle", "maven", "npm", "none"
    val languages: List<String>,
    val frameworks: List<String>,
    val dependencies: List<String>,
    val additionalProperties: Map<String, String> = emptyMap()
)
