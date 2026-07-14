package com.aistudio.xide.core.workspace.data

import com.aistudio.xide.core.workspace.WorkspaceSession
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    suspend fun createProject(project: ProjectEntity)
    suspend fun getProject(projectId: String): ProjectEntity?
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    suspend fun addRecentProject(projectId: String)
    fun getRecentProjects(): Flow<List<RecentProjectEntity>>
    
    suspend fun saveWorkspaceSession(session: WorkspaceSessionEntity)
    suspend fun getLatestSession(projectId: String): WorkspaceSessionEntity?
    
    suspend fun saveMetadata(metadata: WorkspaceMetadataEntity)
    suspend fun getMetadata(projectId: String): List<WorkspaceMetadataEntity>
}
