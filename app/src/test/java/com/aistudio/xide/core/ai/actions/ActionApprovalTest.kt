package com.aistudio.xide.core.ai.actions

import org.junit.Assert.*
import org.junit.Test

class ActionApprovalTest {

    @Test
    fun testActionApprovalWorkflow() {
        val manager = ActionApprovalManager()
        val action = AIAction.CreateFile("act-1", System.currentTimeMillis(), "Xero", "test.kt", "code")

        val state = manager.proposeAction(action)
        assertEquals(ActionApprovalState.PENDING, state)
        assertEquals(ActionApprovalState.PENDING, manager.getActionState("act-1"))

        manager.approveAction("act-1")
        assertEquals(ActionApprovalState.APPROVED, manager.getActionState("act-1"))

        manager.updateState("act-1", ActionApprovalState.EXECUTING)
        assertEquals(ActionApprovalState.EXECUTING, manager.getActionState("act-1"))

        manager.updateState("act-1", ActionApprovalState.COMPLETED)
        assertEquals(ActionApprovalState.COMPLETED, manager.getActionState("act-1"))
    }

    @Test
    fun testActionRejection() {
        val manager = ActionApprovalManager()
        val action = AIAction.ModifyFile("act-2", System.currentTimeMillis(), "Xero", "test.kt", "new-code")

        val state = manager.proposeAction(action)
        assertEquals(ActionApprovalState.PENDING, state)

        manager.rejectAction("act-2")
        assertEquals(ActionApprovalState.REJECTED, manager.getActionState("act-2"))
    }

    @Test
    fun testAutoApprovedAction() {
        val manager = ActionApprovalManager()
        val action = AIAction.ExplainCode("act-3", System.currentTimeMillis(), "Xero", "val a = 1")

        val state = manager.proposeAction(action)
        assertEquals(ActionApprovalState.APPROVED, state) // Auto-approved because READ_ONLY
    }
}
