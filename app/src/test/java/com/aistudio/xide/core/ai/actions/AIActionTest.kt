package com.aistudio.xide.core.ai.actions

import org.junit.Assert.*
import org.junit.Test

class AIActionTest {

    @Test
    fun testActionCreationAndMetadata() {
        val action = AIAction.CreateFile(
            actionId = "test-123",
            timestamp = 1000L,
            source = "User",
            path = "/workspace/file.kt",
            content = "class A"
        )

        assertEquals("test-123", action.actionId)
        assertEquals(1000L, action.timestamp)
        assertEquals("User", action.source)
        assertEquals("/workspace/file.kt", action.path)
        assertEquals("class A", action.content)
        assertEquals(listOf("/workspace/file.kt"), action.targetFiles)
    }

    @Test
    fun testRiskClassification() {
        val create = AIAction.CreateFile("1", 0L, "Xero", "f.kt", "")
        val modify = AIAction.ModifyFile("2", 0L, "Xero", "f.kt", "")
        val delete = AIAction.DeleteFile("3", 0L, "Xero", "f.kt")
        val rename = AIAction.RenameFile("4", 0L, "Xero", "f1.kt", "f2.kt")
        val explain = AIAction.ExplainCode("5", 0L, "Xero", "class A")

        assertEquals(RiskLevel.MEDIUM, create.riskLevel)
        assertEquals(RiskLevel.HIGH, modify.riskLevel)
        assertEquals(RiskLevel.HIGH, delete.riskLevel)
        assertEquals(RiskLevel.MEDIUM, rename.riskLevel)
        assertEquals(RiskLevel.LOW, explain.riskLevel)

        assertEquals(ApprovalRequirement.EXPLICIT_CONFIRMATION, create.approvalRequirement)
        assertEquals(ApprovalRequirement.EXPLICIT_CONFIRMATION, modify.approvalRequirement)
        assertEquals(ApprovalRequirement.EXPLICIT_CONFIRMATION, delete.approvalRequirement)
        assertEquals(ApprovalRequirement.EXPLICIT_CONFIRMATION, rename.approvalRequirement)
        assertEquals(ApprovalRequirement.READ_ONLY, explain.approvalRequirement)
    }
}
