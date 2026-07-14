package com.aistudio.xide.core.xero

import java.util.concurrent.ConcurrentHashMap

data class XeroMemory(
    val projectSummaries: Map<String, String>,
    val previousActions: List<String>,
    val successfulSolutions: Map<String, String>,
    val userPreferences: Map<String, String>
)

interface XeroMemoryContext {
    suspend fun getMemory(): XeroMemory
    suspend fun recordAction(action: String)
    suspend fun recordSolution(problem: String, solution: String)
}

class DefaultXeroMemoryContext : XeroMemoryContext {
    private val previousActions = mutableListOf<String>()
    private val successfulSolutions = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val projectSummaries = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val userPreferences = java.util.concurrent.ConcurrentHashMap<String, String>()

    override suspend fun getMemory(): XeroMemory {
        return XeroMemory(
            projectSummaries = projectSummaries.toMap(),
            previousActions = previousActions.toList(),
            successfulSolutions = successfulSolutions.toMap(),
            userPreferences = userPreferences.toMap()
        )
    }

    override suspend fun recordAction(action: String) {
        previousActions.add(action)
    }

    override suspend fun recordSolution(problem: String, solution: String) {
        successfulSolutions[problem] = solution
    }
}
