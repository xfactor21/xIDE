package com.aistudio.xide.core.xero.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ConversationHistoryTest {

    @Test
    fun testHistoryLimitsAndSecurity() {
        val history = ConversationHistory(maxCapacity = 3)

        // 1. Add normal entries
        history.addEntry(ConversationEntry("What is xIDE?", DeveloperIntent.EXPLAIN_CODE, emptyList(), "Explanation", 100L))
        history.addEntry(ConversationEntry("How to index?", DeveloperIntent.EXPLAIN_CODE, emptyList(), "Explanation", 200L))
        history.addEntry(ConversationEntry("Find definition", DeveloperIntent.FIND_SYMBOL, emptyList(), "SearchResult", 300L))
        
        assertEquals(3, history.getHistory().size)

        // 2. Exceed limit and verify oldest is evicted (FIFO)
        history.addEntry(ConversationEntry("Latest query", DeveloperIntent.SUGGEST_IMPROVEMENT, emptyList(), "CodeInsight", 400L))
        val current = history.getHistory()
        assertEquals(3, current.size)
        assertEquals("How to index?", current[0].question)
        assertEquals("Latest query", current[2].question)

        // 3. Test secret filtering / redaction
        history.addEntry(ConversationEntry("Connect using API_KEY=AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q and secret_token=my_secret_token", DeveloperIntent.EXPLAIN_CODE, emptyList(), "Explanation", 500L))
        val lastEntry = history.getHistory().last()
        assertFalse("API Key should be redacted", lastEntry.question.contains("AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q"))
        assertFalse("Secret token should be redacted", lastEntry.question.contains("my_secret_token"))

        // 4. Test cleanup
        history.clearHistory()
        assertTrue(history.getHistory().isEmpty())
    }
}
