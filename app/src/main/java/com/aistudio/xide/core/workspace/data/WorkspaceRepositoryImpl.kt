package com.aistudio.xide.core.workspace.data

import kotlinx.coroutines.flow.Flow

class WorkspaceRepositoryImpl(
    private val workspaceDao: WorkspaceDao
) : WorkspaceRepository {
    override suspend fun createProject(project: ProjectEntity) {
        workspaceDao.insertProject(project)
    }

    override suspend fun getProject(projectId: String): ProjectEntity? {
        return workspaceDao.getProject(projectId)
    }

    override fun getAllProjects(): Flow<List<ProjectEntity>> {
        return workspaceDao.getAllProjects()
    }

    override suspend fun addRecentProject(projectId: String) {
        workspaceDao.insertRecentProject(
            RecentProjectEntity(
                projectId = projectId,
                lastOpenedAt = System.currentTimeMillis(),
                isPinned = false
            )
        )
    }

    override fun getRecentProjects(): Flow<List<RecentProjectEntity>> {
        return workspaceDao.getRecentProjects()
    }

    override suspend fun saveWorkspaceSession(session: WorkspaceSessionEntity) {
        workspaceDao.saveWorkspaceSession(session)
    }

    override suspend fun getLatestSession(projectId: String): WorkspaceSessionEntity? {
        return workspaceDao.getLatestSessionForProject(projectId)
    }

    override suspend fun saveMetadata(metadata: WorkspaceMetadataEntity) {
        workspaceDao.insertMetadata(metadata)
    }

    override suspend fun getMetadata(projectId: String): List<WorkspaceMetadataEntity> {
        return workspaceDao.getMetadataForProject(projectId)
    }
}
