package com.aistudio.xide.core.ai.actions

import org.junit.Assert.*
import org.junit.Test

class ChangePreviewTest {

    @Test
    fun testChangePreviewDiffAndHash() {
        val original = "package com.example\n\nfun hello() {\n    println(\"hello\")\n}"
        val proposed = "package com.example\n\nfun hello() {\n    println(\"hello modified\")\n    println(\"added line\")\n}"

        val preview = ChangePreview.generate("MainActivity.kt", original, proposed)

        assertEquals(listOf("MainActivity.kt"), preview.affectedFiles)
        assertNotEquals("", preview.originalContentHash)
        assertEquals(2, preview.linesAdded)
        assertEquals(1, preview.linesDeleted)
        assertTrue(preview.estimatedImpact.startsWith("LOW"))
    }

    @Test
    fun testHighImpactRating() {
        val original = "line1\nline2"
        val proposed = (1..150).joinToString("\n") { "new line $it" }

        val preview = ChangePreview.generate("LargeFile.kt", original, proposed)
        assertTrue(preview.estimatedImpact.startsWith("HIGH"))
    }
}
