package com.aistudio.xide.core.intelligence

/**
 * The brain of xIDE, responsible for context awareness and architecture understanding.
 */
interface ProjectIntelligence {
    suspend fun indexProject(rootPath: String)
    suspend fun getProjectArchitecture(): ProjectArchitecture
    suspend fun findUsages(symbol: String): List<String>
    suspend fun getBuildHistory(): List<BuildRecord>
}

data class ProjectArchitecture(
    val language: String,
    val framework: String,
    val modules: List<String>
)

data class BuildRecord(
    val timestamp: Long,
    val success: Boolean,
    val errorSummary: String?
)
