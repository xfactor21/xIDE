package com.aistudio.xide.core.xero

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentXeroMemoryContextTest {

    private class FakeXeroMemoryDao : XeroMemoryDao {
        val actions = mutableListOf<XeroActionEntity>()
        val solutions = mutableListOf<XeroSolutionEntity>()
        val summaries = mutableListOf<XeroSummaryEntity>()
        val preferences = mutableListOf<XeroPreferenceEntity>()

        override suspend fun insertAction(action: XeroActionEntity) {
            actions.add(action)
        }

        override suspend fun getAllActions(): List<String> {
            return actions.sortedBy { it.timestamp }.map { it.action }
        }

        override suspend fun insertSolution(solution: XeroSolutionEntity) {
            solutions.add(solution)
        }

        override suspend fun getAllSolutions(): List<XeroSolutionEntity> {
            return solutions
        }

        override suspend fun insertSummary(summary: XeroSummaryEntity) {
            summaries.add(summary)
        }

        override suspend fun getAllSummaries(): List<XeroSummaryEntity> {
            return summaries
        }

        override suspend fun insertPreference(preference: XeroPreferenceEntity) {
            preferences.add(preference)
        }

        override suspend fun getAllPreferences(): List<XeroPreferenceEntity> {
            return preferences
        }
    }

    @Test
    fun testRecordAndRetrieveMemory() = runBlocking {
        val fakeDao = FakeXeroMemoryDao()
        val context = PersistentXeroMemoryContext(fakeDao)

        context.recordAction("Action A")
        context.recordAction("Action B")
        context.recordSolution("Problem A", "Solution A")
        context.recordPreference("theme", "dark")
        context.recordSummary("project_1", "Comprehensive code update")

        val memory = context.getMemory()

        assertEquals(2, memory.previousActions.size)
        assertEquals("Action A", memory.previousActions[0])
        assertEquals("Action B", memory.previousActions[1])

        assertEquals(1, memory.successfulSolutions.size)
        assertEquals("Solution A", memory.successfulSolutions["Problem A"])

        assertEquals(1, memory.userPreferences.size)
        assertEquals("dark", memory.userPreferences["theme"])

        assertEquals(1, memory.projectSummaries.size)
        assertEquals("Comprehensive code update", memory.projectSummaries["project_1"])
    }
}
