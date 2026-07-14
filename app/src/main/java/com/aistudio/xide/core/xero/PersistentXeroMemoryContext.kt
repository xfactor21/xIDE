package com.aistudio.xide.core.xero

class PersistentXeroMemoryContext(
    private val dao: XeroMemoryDao
) : XeroMemoryContext {

    override suspend fun getMemory(): XeroMemory {
        val actions = dao.getAllActions()
        val solutions = dao.getAllSolutions().associate { it.problem to it.solution }
        val summaries = dao.getAllSummaries().associate { it.projectId to it.summary }
        val preferences = dao.getAllPreferences().associate { it.key to it.value }
        return XeroMemory(
            projectSummaries = summaries,
            previousActions = actions,
            successfulSolutions = solutions,
            userPreferences = preferences
        )
    }

    override suspend fun recordAction(action: String) {
        dao.insertAction(XeroActionEntity(action = action))
    }

    override suspend fun recordSolution(problem: String, solution: String) {
        dao.insertSolution(XeroSolutionEntity(problem = problem, solution = solution))
    }

    suspend fun recordPreference(key: String, value: String) {
        dao.insertPreference(XeroPreferenceEntity(key = key, value = value))
    }

    suspend fun recordSummary(projectId: String, summary: String) {
        dao.insertSummary(XeroSummaryEntity(projectId = projectId, summary = summary))
    }
}
