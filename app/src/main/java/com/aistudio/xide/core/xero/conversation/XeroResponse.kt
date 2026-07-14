package com.aistudio.xide.core.xero.conversation

import com.aistudio.xide.core.ai.actions.AIAction

sealed class XeroResponse {
    abstract val summary: String
    abstract val confidence: Double
    abstract val supportingFiles: List<String>
    abstract val relatedSymbols: List<String>
    abstract val nextSteps: List<String>

    data class Explanation(
        override val summary: String,
        override val confidence: Double,
        override val supportingFiles: List<String>,
        override val relatedSymbols: List<String>,
        override val nextSteps: List<String>
    ) : XeroResponse()

    data class DiagnosticReport(
        override val summary: String,
        override val confidence: Double,
        override val supportingFiles: List<String>,
        override val relatedSymbols: List<String>,
        override val nextSteps: List<String>,
        val diagnostics: List<String>
    ) : XeroResponse()

    data class SearchResult(
        override val summary: String,
        override val confidence: Double,
        override val supportingFiles: List<String>,
        override val relatedSymbols: List<String>,
        override val nextSteps: List<String>,
        val results: List<String>
    ) : XeroResponse()

    data class CodeInsight(
        override val summary: String,
        override val confidence: Double,
        override val supportingFiles: List<String>,
        override val relatedSymbols: List<String>,
        override val nextSteps: List<String>,
        val patterns: List<String>
    ) : XeroResponse()

    data class ActionProposal(
        override val summary: String,
        override val confidence: Double,
        override val supportingFiles: List<String>,
        override val relatedSymbols: List<String>,
        override val nextSteps: List<String>,
        val proposedActions: List<AIAction>
    ) : XeroResponse()
}
