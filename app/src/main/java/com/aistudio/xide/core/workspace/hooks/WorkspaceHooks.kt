package com.aistudio.xide.core.workspace.hooks

interface DiagnosticsHook {
    fun checkWorkspaceIntegrity(projectId: String): Boolean
    fun checkStorageHealth(): Boolean
    fun checkIndexStatus(projectId: String): Boolean
}

interface AutomationHook {
    fun scheduleIndexing(projectId: String)
    fun scheduleRecovery(projectId: String)
    fun scheduleCacheRebuild(projectId: String)
}

interface ProjectIntelligenceHook {
    fun requestProjectIndexing(projectId: String)
    fun generateContext(projectId: String): String
}
