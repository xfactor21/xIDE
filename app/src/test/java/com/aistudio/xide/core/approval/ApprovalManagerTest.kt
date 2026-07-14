package com.aistudio.xide.core.approval

import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovalManagerTest {
    @Test
    fun testRequestApprovalAndApprove() {
        val manager = DefaultApprovalManager()
        val req = manager.requestApproval("Test", emptyList(), emptyList())
        
        assertEquals(ApprovalState.REQUESTED, req.state)
        assertEquals(1, manager.getPendingRequests().size)
        
        manager.approve(req.requestId)
        
        assertEquals(0, manager.getPendingRequests().size)
        assertEquals(ApprovalState.APPROVED, manager.observeRequest(req.requestId)?.value)
    }

    @Test
    fun testRequestApprovalAndReject() {
        val manager = DefaultApprovalManager()
        val req = manager.requestApproval("Test", emptyList(), emptyList())
        
        manager.reject(req.requestId)
        
        assertEquals(0, manager.getPendingRequests().size)
        assertEquals(ApprovalState.REJECTED, manager.observeRequest(req.requestId)?.value)
    }
}
