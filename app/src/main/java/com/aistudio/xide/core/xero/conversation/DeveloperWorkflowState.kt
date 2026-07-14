package com.aistudio.xide.core.xero.conversation

import kotlinx.coroutines.flow.StateFlow

enum class DeveloperWorkflowState {
    IDLE,
    ANALYZING_PROJECT,
    UNDERSTANDING_REQUEST,
    GENERATING_RESPONSE,
    WAITING_APPROVAL,
    EXECUTING_CHANGE,
    VERIFYING_BUILD,
    COMPLETED,
    FAILED
}

/**
 * Interface representing a workflow tracker that allows state monitoring.
 */
interface WorkflowStateTracker {
    val currentState: StateFlow<DeveloperWorkflowState>
    fun updateState(state: DeveloperWorkflowState)
}
