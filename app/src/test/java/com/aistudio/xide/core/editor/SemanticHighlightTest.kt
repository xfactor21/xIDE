package com.aistudio.xide.core.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticHighlightTest {
    @Test
    fun testHighlightExtraction() {
        val provider = SemanticHighlightProvider(null)
        val content = """
            class MyService {
                fun performAction() {
                }
            }
        """.trimIndent()
        
        val highlights = provider.getHighlights("/MyService.kt", content)
        assertEquals(2, highlights.size)
        
        val classHighlight = highlights.find { it.type == HighlightType.CLASS }
        assertEquals(HighlightType.CLASS, classHighlight?.type)
        
        val funHighlight = highlights.find { it.type == HighlightType.FUNCTION }
        assertEquals(HighlightType.FUNCTION, funHighlight?.type)
    }
}
