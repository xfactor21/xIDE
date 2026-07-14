package com.aistudio.xide.core.xero.conversation

import kotlinx.coroutines.flow.StateFlow

interface XeroConversationEngine {
    val assistantState: StateFlow<XeroAssistantState>
    val workflowState: StateFlow<DeveloperWorkflowState>
    val conversationHistory: ConversationHistory

    suspend fun handleDeveloperRequest(
        question: String,
        projectPath: String,
        activeFile: String? = null
    ): XeroResponse
}
