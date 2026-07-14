package com.aistudio.xide.core.workspace

import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.workspace.data.ProjectEntity
import com.aistudio.xide.core.workspace.data.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class WorkspaceManagerImpl(
    private val repository: WorkspaceRepository
) : WorkspaceManager {

    override val providerId: String = "core.workspace.manager"
    override val providerName: String = "Workspace Manager"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("project_lifecycle", "workspace_session")
    override val requirements: List<String> = listOf("vfs")
    override val limitations: List<String> = emptyList()
    override val description: String = "Workspace state manager"
    override val author: String = "xIDE"
    override val compatibilityVersion: String = "1.0.0"

    private var activeSession: WorkspaceSession? = null

    override suspend fun healthCheck(): ProviderHealth {
        return ProviderHealth.HEALTHY
    }

    override suspend fun initialize() {
        // Initialization logic
    }

    override suspend fun shutdown() {
        // Cleanup logic
        closeCurrentProject()
    }

    override fun createProject(name: String, path: String) {
        val projectId = UUID.randomUUID().toString()
        val project = ProjectEntity(
            projectId = projectId,
            name = name,
            rootPath = path,
            fileSystemProtocol = "local",
            createdAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )
        CoroutineScope(Dispatchers.IO).launch {
            repository.createProject(project)
        }
    }

    override fun openProject(projectId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.addRecentProject(projectId)
            // Load session
            val sessionEntity = repository.getLatestSession(projectId)
            activeSession = WorkspaceSession(
                sessionId = sessionEntity?.sessionId ?: UUID.randomUUID().toString(),
                projectId = projectId,
                openFiles = emptyList(), // Decode from JSON in real implementation
                activeFile = sessionEntity?.activeFilePath
            )
        }
    }

    override fun closeCurrentProject() {
        activeSession = null
    }

    override fun getActiveWorkspace(): WorkspaceSession? {
        return activeSession
    }
}
