package com.aistudio.xide.core.workspace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.xide.core.approval.ApprovalManager
import com.aistudio.xide.core.approval.ApprovalRequest
import com.aistudio.xide.core.automation.TaskQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkspaceAutomationState(
    val activeTasks: Int = 0,
    val pendingApprovals: List<ApprovalRequest> = emptyList(),
    val isBuilding: Boolean = false
)

class WorkspaceAutomationViewModel(
    private val approvalManager: ApprovalManager,
    private val taskQueue: TaskQueue
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceAutomationState())
    val state: StateFlow<WorkspaceAutomationState> = _state.asStateFlow()

    init {
        refreshPendingApprovals()
    }

    fun refreshPendingApprovals() {
        _state.value = _state.value.copy(
            pendingApprovals = approvalManager.getPendingRequests()
        )
    }

    fun approveRequest(requestId: String) {
        approvalManager.approve(requestId)
        refreshPendingApprovals()
    }

    fun rejectRequest(requestId: String) {
        approvalManager.reject(requestId)
        refreshPendingApprovals()
    }
}
