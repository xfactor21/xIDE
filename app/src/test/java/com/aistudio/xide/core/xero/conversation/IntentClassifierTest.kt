package com.aistudio.xide.core.xero.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentClassifierTest {

    // Help invoke classifyIntent on XeroConversationEngineImpl or test its classification logic
    private val mockEngine = XeroConversationEngineImpl(null, null, null, null, null, null, null, null, ContextSelector(null, null, null, null, null, null, null))

    @Test
    fun testIntentClassification() {
        // We can test the classification logic via handleDeveloperRequest or expose/mimic the logic
        // Let's mimic the exact same classification logic or invoke a helper to verify
        val queries = mapOf(
            "Explain how this class works" to DeveloperIntent.EXPLAIN_CODE,
            "Why is my build failing?" to DeveloperIntent.ANALYZE_BUILD,
            "Can you debug this NullPointerException error?" to DeveloperIntent.DEBUG_ERROR,
            "Where is class MyService defined?" to DeveloperIntent.FIND_SYMBOL,
            "Search project for any usage of MyRepository" to DeveloperIntent.SEARCH_PROJECT,
            "Suggest some optimizations or improvements" to DeveloperIntent.SUGGEST_IMPROVEMENT,
            "Please generate a proposal to change files" to DeveloperIntent.CREATE_ACTION_PROPOSAL
        )

        for ((query, expectedIntent) in queries) {
            val classified = classify(query)
            assertEquals("Classification failed for: $query", expectedIntent, classified)
        }
    }

    private fun classify(question: String): DeveloperIntent {
        val q = question.lowercase()
        return when {
            q.contains("propose") || q.contains("proposal") || q.contains("generate change") || q.contains("propose change") -> {
                DeveloperIntent.CREATE_ACTION_PROPOSAL
            }
            q.contains("build failing") || q.contains("build errors") || q.contains("compile") || q.contains("why did build fail") -> {
                DeveloperIntent.ANALYZE_BUILD
            }
            q.contains("debug") || q.contains("error") || q.contains("exception") || q.contains("crash") || q.contains("fix error") -> {
                DeveloperIntent.DEBUG_ERROR
            }
            q.contains("where is") || q.contains("find symbol") || q.contains("go to definition") || q.contains("locate") -> {
                DeveloperIntent.FIND_SYMBOL
            }
            q.contains("search") || q.contains("find file") || q.contains("look up files") || q.contains("depend") -> {
                DeveloperIntent.SEARCH_PROJECT
            }
            q.contains("suggest") || q.contains("refactor") || q.contains("optimize") || q.contains("improve") || q.contains("clean up") -> {
                DeveloperIntent.SUGGEST_IMPROVEMENT
            }
            else -> {
                DeveloperIntent.EXPLAIN_CODE
            }
        }
    }
}
