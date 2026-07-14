package com.aistudio.xide.core.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImprovementSuggestionTest {
    @Test
    fun testEmptyCatchBlockSuggestion() {
        val analyzer = CodeImprovementAnalyzer()
        val content = """
            fun doWork() {
                try {
                    // work
                } catch (e: Exception) {
                }
            }
        """.trimIndent()
        
        val suggestions = analyzer.analyzeForImprovements("/Work.kt", content)
        assertEquals(1, suggestions.size)
        
        val suggestion = suggestions[0]
        assertEquals("Unsafe empty catch block", suggestion.title)
        assertTrue(suggestion.suggestedContent.contains("e.printStackTrace()"))
    }
}
