package com.aistudio.xide.core.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LoadRecentProjects -> {
                _state.update { it.copy(isLoading = true) }
                // Implementation will be handled in Phase 3
            }
            is DashboardEvent.ProjectSelected -> {
                // Implementation will be handled in Phase 3
            }
        }
    }
}
