package com.aistudio.xide.core.workspace.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE projectId = :projectId")
    suspend fun getProject(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentProject(recentProject: RecentProjectEntity)

    @Query("SELECT * FROM recent_projects ORDER BY lastOpenedAt DESC")
    fun getRecentProjects(): Flow<List<RecentProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWorkspaceSession(session: WorkspaceSessionEntity)

    @Query("SELECT * FROM workspace_sessions WHERE projectId = :projectId ORDER BY savedAt DESC LIMIT 1")
    suspend fun getLatestSessionForProject(projectId: String): WorkspaceSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: WorkspaceMetadataEntity)

    @Query("SELECT * FROM workspace_metadata WHERE projectId = :projectId")
    suspend fun getMetadataForProject(projectId: String): List<WorkspaceMetadataEntity>
}
