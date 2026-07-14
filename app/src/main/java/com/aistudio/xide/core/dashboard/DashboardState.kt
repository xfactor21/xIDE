package com.aistudio.xide.core.dashboard

import com.aistudio.xide.ui.architecture.UiState
import com.aistudio.xide.ui.architecture.UiEvent
import com.aistudio.xide.ui.architecture.UiEffect

data class DashboardState(
    val recentProjects: List<String> = emptyList(),
    val isLoading: Boolean = false
) : UiState

sealed interface DashboardEvent : UiEvent {
    object LoadRecentProjects : DashboardEvent
    data class ProjectSelected(val projectId: String) : DashboardEvent
}

sealed interface DashboardEffect : UiEffect {
    data class NavigateToProject(val projectId: String) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}
