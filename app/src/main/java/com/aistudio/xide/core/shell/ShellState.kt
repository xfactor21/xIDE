package com.aistudio.xide.core.shell

import com.aistudio.xide.ui.architecture.UiState
import com.aistudio.xide.core.xero.XeroState

data class ShellState(
    val navigationState: NavigationState = NavigationState(),
    val workspaceState: WorkspaceState = WorkspaceState(),
    val statusState: StatusState = StatusState(),
    val xeroState: XeroState = XeroState.Idle,
    val notificationState: NotificationState = NotificationState()
) : UiState

data class NavigationState(
    val currentRoute: String = "dashboard"
)

data class WorkspaceState(
    val isWorkspaceLoaded: Boolean = false,
    val activeWorkspaceId: String? = null
)

data class StatusState(
    val isBusy: Boolean = false,
    val statusMessage: String = "Ready"
)

data class NotificationState(
    val notifications: List<String> = emptyList()
)
