package com.aistudio.xide.core.xero.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class XeroAssistantStateTest {

    @Test
    fun testStateTransitions() {
        val stateFlow = MutableStateFlow(XeroAssistantState.IDLE)
        assertEquals(XeroAssistantState.IDLE, stateFlow.value)

        // Simulate xIDE conversation steps
        stateFlow.value = XeroAssistantState.THINKING
        assertEquals(XeroAssistantState.THINKING, stateFlow.value)

        stateFlow.value = XeroAssistantState.ANALYZING
        assertEquals(XeroAssistantState.ANALYZING, stateFlow.value)

        stateFlow.value = XeroAssistantState.RESPONDING
        assertEquals(XeroAssistantState.RESPONDING, stateFlow.value)

        stateFlow.value = XeroAssistantState.WAITING_APPROVAL
        assertEquals(XeroAssistantState.WAITING_APPROVAL, stateFlow.value)

        stateFlow.value = XeroAssistantState.ERROR
        assertEquals(XeroAssistantState.ERROR, stateFlow.value)
    }
}
