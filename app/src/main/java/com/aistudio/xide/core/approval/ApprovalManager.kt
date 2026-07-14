package com.aistudio.xide.core.approval

import com.aistudio.xide.core.command.ActionDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class ApprovalState {
    REQUESTED,
    APPROVED,
    REJECTED,
    EXECUTING,
    COMPLETED,
    FAILED
}

data class ApprovalRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val description: String,
    val commands: List<ActionDescriptor>,
    val state: ApprovalState = ApprovalState.REQUESTED,
    val requiredPermissions: List<String> = emptyList()
)

interface ApprovalManager {
    fun requestApproval(description: String, commands: List<ActionDescriptor>, permissions: List<String>): ApprovalRequest
    fun approve(requestId: String)
    fun reject(requestId: String)
    fun updateState(requestId: String, state: ApprovalState)
    fun observeRequest(requestId: String): StateFlow<ApprovalState>?
    fun getPendingRequests(): List<ApprovalRequest>
}

class DefaultApprovalManager : ApprovalManager {
    private val requests = ConcurrentHashMap<String, ApprovalRequest>()
    private val stateFlows = ConcurrentHashMap<String, MutableStateFlow<ApprovalState>>()

    override fun requestApproval(description: String, commands: List<ActionDescriptor>, permissions: List<String>): ApprovalRequest {
        val request = ApprovalRequest(
            description = description,
            commands = commands,
            requiredPermissions = permissions
        )
        requests[request.requestId] = request
        stateFlows[request.requestId] = MutableStateFlow(ApprovalState.REQUESTED)
        return request
    }

    override fun approve(requestId: String) {
        val req = requests[requestId] ?: return
        if (req.state == ApprovalState.REQUESTED) {
            requests[requestId] = req.copy(state = ApprovalState.APPROVED)
            stateFlows[requestId]?.value = ApprovalState.APPROVED
        }
    }

    override fun reject(requestId: String) {
        val req = requests[requestId] ?: return
        if (req.state == ApprovalState.REQUESTED) {
            requests[requestId] = req.copy(state = ApprovalState.REJECTED)
            stateFlows[requestId]?.value = ApprovalState.REJECTED
        }
    }

    override fun updateState(requestId: String, state: ApprovalState) {
        val req = requests[requestId] ?: return
        requests[requestId] = req.copy(state = state)
        stateFlows[requestId]?.value = state
    }

    override fun observeRequest(requestId: String): StateFlow<ApprovalState>? {
        return stateFlows[requestId]?.asStateFlow()
    }

    override fun getPendingRequests(): List<ApprovalRequest> {
        return requests.values.filter { it.state == ApprovalState.REQUESTED }
    }
}
