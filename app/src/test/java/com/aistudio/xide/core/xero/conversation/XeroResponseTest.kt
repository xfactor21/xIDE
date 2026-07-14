package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.ai.actions.AIAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XeroResponseTest {

    @Test
    fun testResponseCreationAndTypes() {
        // 1. Explanation type
        val exp: XeroResponse = XeroResponse.Explanation(
            summary = "Explains the controller class",
            confidence = 0.95,
            supportingFiles = listOf("Controller.kt"),
            relatedSymbols = listOf("Controller"),
            nextSteps = listOf("Highlight a function")
        )
        assertEquals("Explains the controller class", exp.summary)
        assertEquals(0.95, exp.confidence, 0.001)
        assertTrue(exp.supportingFiles.contains("Controller.kt"))

        // 2. ActionProposal type
        val propAction = AIAction.ExplainCode("action-1", 1000L, "Xero", "class Controller")
        val proposal: XeroResponse = XeroResponse.ActionProposal(
            summary = "Proposed 1 action to explain code",
            confidence = 0.8,
            supportingFiles = listOf("Controller.kt"),
            relatedSymbols = emptyList(),
            nextSteps = emptyList(),
            proposedActions = listOf(propAction)
        )
        assertEquals("Proposed 1 action to explain code", proposal.summary)
        assertTrue((proposal as XeroResponse.ActionProposal).proposedActions.contains(propAction))
    }
}
