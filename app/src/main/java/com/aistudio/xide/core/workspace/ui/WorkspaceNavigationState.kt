package com.aistudio.xide.core.workspace.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkspaceNavigationState(
    val activeFile: String? = null,
    val selectedPanel: String = "explorer", // "explorer", "xero", "diagnostics", "build"
    val openFiles: List<String> = emptyList(),
    val currentProjectId: String? = null,
    val currentProjectName: String? = null,
    val currentProjectRoot: String? = null
)

class WorkspaceNavigationManager {
    private val _navigationState = MutableStateFlow(WorkspaceNavigationState())
    val navigationState: StateFlow<WorkspaceNavigationState> = _navigationState.asStateFlow()

    fun selectFile(filePath: String) {
        val currentOpen = _navigationState.value.openFiles
        val newOpen = if (filePath !in currentOpen) currentOpen + filePath else currentOpen
        _navigationState.value = _navigationState.value.copy(
            activeFile = filePath,
            openFiles = newOpen
        )
    }

    fun closeFile(filePath: String) {
        val currentOpen = _navigationState.value.openFiles.filter { it != filePath }
        val currentActive = _navigationState.value.activeFile
        val newActive = if (currentActive == filePath) {
            if (currentOpen.isNotEmpty()) currentOpen.last() else null
        } else {
            currentActive
        }
        _navigationState.value = _navigationState.value.copy(
            activeFile = newActive,
            openFiles = currentOpen
        )
    }

    fun selectPanel(panel: String) {
        _navigationState.value = _navigationState.value.copy(
            selectedPanel = panel
        )
    }

    fun setCurrentProject(projectId: String?, projectName: String?, projectRoot: String?) {
        _navigationState.value = _navigationState.value.copy(
            currentProjectId = projectId,
            currentProjectName = projectName,
            currentProjectRoot = projectRoot
        )
    }
}
