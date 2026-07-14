package com.aistudio.xide.core.workspace.ui

import com.aistudio.xide.core.approval.ApprovalManager
import com.aistudio.xide.core.approval.ApprovalRequest
import com.aistudio.xide.core.approval.ApprovalState
import com.aistudio.xide.core.command.ActionDescriptor
import com.aistudio.xide.core.automation.TaskQueue
import com.aistudio.xide.core.automation.AutomationTask
import com.aistudio.xide.core.automation.TaskStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAutomationViewModelTest {
    @Test
    fun testViewModelState() {
        val requests = mutableListOf<ApprovalRequest>()
        val approvalManager = object : ApprovalManager {
            override fun requestApproval(description: String, commands: List<ActionDescriptor>, permissions: List<String>): ApprovalRequest {
                val req = ApprovalRequest(description = description, commands = commands, requiredPermissions = permissions)
                requests.add(req)
                return req
            }
            override fun approve(requestId: String) {
                val req = requests.find { it.requestId == requestId }
                if (req != null) {
                    requests.remove(req)
                    requests.add(req.copy(state = ApprovalState.APPROVED))
                }
            }
            override fun reject(requestId: String) {}
            override fun updateState(requestId: String, state: ApprovalState) {}
            override fun observeRequest(requestId: String): StateFlow<ApprovalState>? = null
            override fun getPendingRequests(): List<ApprovalRequest> = requests.filter { it.state == ApprovalState.REQUESTED }
        }

        val taskQueue = object : TaskQueue {
            override fun enqueue(task: AutomationTask) {}
            override fun cancel(taskId: String) {}
            override fun observeTaskStatus(taskId: String): StateFlow<TaskStatus> = MutableStateFlow(TaskStatus.QUEUED)
            override fun observeTaskProgress(taskId: String): StateFlow<Float> = MutableStateFlow(0f)
        }

        val viewModel = WorkspaceAutomationViewModel(approvalManager, taskQueue)
        
        approvalManager.requestApproval("test", emptyList(), emptyList())
        viewModel.refreshPendingApprovals()
        
        assertEquals(1, viewModel.state.value.pendingApprovals.size)
        
        val reqId = viewModel.state.value.pendingApprovals[0].requestId
        viewModel.approveRequest(reqId)
        
        assertEquals(0, viewModel.state.value.pendingApprovals.size)
    }
}
