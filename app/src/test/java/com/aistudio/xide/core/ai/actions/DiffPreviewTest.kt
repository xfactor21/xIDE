package com.aistudio.xide.core.ai.actions

import org.junit.Test
import org.junit.Assert.*

class DiffPreviewTest {
    @Test
    fun testAddedLines() {
        val diff = ChangeDiffModel(
            originalContent = "a",
            proposedContent = "a\nb",
            addedLines = 1,
            removedLines = 0,
            changedSections = emptyList()
        )
        assertEquals(1, diff.addedLines)
    }

    @Test
    fun testRemovedLines() {
        val diff = ChangeDiffModel(
            originalContent = "a\nb",
            proposedContent = "a",
            addedLines = 0,
            removedLines = 1,
            changedSections = emptyList()
        )
        assertEquals(1, diff.removedLines)
    }

    @Test
    fun testHashValidation() {
        val diff = ChangeDiffModel(
            originalContent = "a",
            proposedContent = "b",
            addedLines = 1,
            removedLines = 1,
            changedSections = listOf(
                DiffSection(DiffType.MODIFIED, 1, 1, 1, 1, "b")
            )
        )
        assertEquals(1, diff.changedSections.size)
        assertEquals(DiffType.MODIFIED, diff.changedSections[0].type)
    }
}
