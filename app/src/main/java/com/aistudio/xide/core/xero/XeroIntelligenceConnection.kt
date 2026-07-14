package com.aistudio.xide.core.xero

import com.aistudio.xide.core.intelligence.ProjectContext
import com.aistudio.xide.core.provider.XideProvider

/**
 * Provides necessary context to the Xero agent for intelligent actions.
 */
interface XeroContextProvider : XideProvider {
    suspend fun getContextForRequest(request: XeroAnalysisRequest): ProjectContext
}

data class XeroAnalysisRequest(
    val id: String,
    val projectPath: String,
    val prompt: String,
    val activeFilePath: String?,
    val activeSelectionRange: String? // Simplification of SelectionRange for architecture layer
)

data class XeroAnalysisResult(
    val requestId: String,
    val analysisSummary: String,
    val proposedActions: List<XeroAction>
)
