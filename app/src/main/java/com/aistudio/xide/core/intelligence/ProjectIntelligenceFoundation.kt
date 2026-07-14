package com.aistudio.xide.core.intelligence

import kotlinx.coroutines.flow.StateFlow

interface ProjectIndexer {
    fun indexProject(projectPath: String): IndexingTask
    suspend fun getIndex(projectPath: String): ProjectIndex?
    suspend fun getContext(projectPath: String): ProjectContext
}

data class ProjectIndex(
    val projectPath: String,
    val files: List<String>,
    val symbols: List<ProjectSymbol>,
    val relationships: List<ProjectRelationship>,
    val metadata: Map<String, String>
)

data class ProjectContext(
    val projectPath: String,
    val activeFiles: List<String>,
    val architectureSummary: String,
    val contextPreparation: String,
    // Additions for Phase 9
    val projectType: String = "unknown",
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val buildSystem: String = "unknown",
    val dependencies: List<String> = emptyList(),
    val activeProblems: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList()
)

data class ProjectSymbol(
    val name: String,
    val type: String, // Class, Function, Variable, etc.
    val location: String, // File path and range
    val language: String,
    val references: List<String>
)

data class ProjectRelationship(
    val sourceSymbol: String,
    val targetSymbol: String,
    val relationshipType: String // e.g., "imports", "dependsOn", "implements"
)

interface IndexingTask {
    val taskId: String
    val projectPath: String
    val progress: StateFlow<Float>
    val status: StateFlow<IndexingStatus>
    suspend fun await()
    fun cancel()
}

enum class IndexingStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELED
}
