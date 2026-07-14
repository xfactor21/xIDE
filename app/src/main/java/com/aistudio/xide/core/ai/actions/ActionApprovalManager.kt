package com.aistudio.xide.core.ai.actions

import java.util.concurrent.ConcurrentHashMap

enum class ActionApprovalState {
    PENDING,
    APPROVED,
    REJECTED,
    EXECUTING,
    COMPLETED,
    FAILED
}

class ActionApprovalManager {
    private val actionStates = ConcurrentHashMap<String, ActionApprovalState>()
    private val proposedActions = ConcurrentHashMap<String, AIAction>()

    fun proposeAction(action: AIAction): ActionApprovalState {
        proposedActions[action.actionId] = action
        val initialState = if (action.approvalRequirement == ApprovalRequirement.EXPLICIT_CONFIRMATION) {
            ActionApprovalState.PENDING
        } else {
            ActionApprovalState.APPROVED
        }
        actionStates[action.actionId] = initialState
        return initialState
    }

    fun approveAction(actionId: String) {
        val current = actionStates[actionId] ?: throw IllegalArgumentException("Action not found: $actionId")
        if (current == ActionApprovalState.PENDING) {
            actionStates[actionId] = ActionApprovalState.APPROVED
        } else {
            throw IllegalStateException("Action is not in PENDING state: $current")
        }
    }

    fun rejectAction(actionId: String) {
        val current = actionStates[actionId] ?: throw IllegalArgumentException("Action not found: $actionId")
        if (current == ActionApprovalState.PENDING) {
            actionStates[actionId] = ActionApprovalState.REJECTED
        } else {
            throw IllegalStateException("Action is not in PENDING state: $current")
        }
    }

    fun updateState(actionId: String, newState: ActionApprovalState) {
        if (!actionStates.containsKey(actionId)) {
            throw IllegalArgumentException("Action not found: $actionId")
        }
        actionStates[actionId] = newState
    }

    fun getActionState(actionId: String): ActionApprovalState? {
        return actionStates[actionId]
    }

    fun getAction(actionId: String): AIAction? {
        return proposedActions[actionId]
    }

    fun getPendingActions(): List<AIAction> {
        return proposedActions.values.filter { actionStates[it.actionId] == ActionApprovalState.PENDING }
    }

    fun getActionHistory(): Map<String, ActionApprovalState> {
        return actionStates.toMap()
    }
}
