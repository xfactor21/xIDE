package com.aistudio.xide.core.ai.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffPreviewExpansionTest {
    @Test
    fun testChangePreviewExpansion() {
        val original = "fun main() {\n    println(\"hello\")\n}"
        val proposed = "fun main() {\n    println(\"hello world\")\n    println(\"done\")\n}"
        
        val preview = ChangePreview.generate("/Main.kt", original, proposed, "Added world and done")
        
        assertEquals(2, preview.linesAdded)
        assertEquals(1, preview.linesDeleted)
        
        assertEquals("Added world and done", preview.changeExplanation)
        assertEquals("kt", preview.fileMetadata["extension"])
        
        assertEquals(1, preview.segments.size)
        assertEquals(SegmentType.MODIFIED, preview.segments[0].type)
    }
}
