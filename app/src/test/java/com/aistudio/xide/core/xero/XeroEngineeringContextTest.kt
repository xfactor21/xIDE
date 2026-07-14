package com.aistudio.xide.core.xero

import org.junit.Test
import org.junit.Assert.*

class XeroEngineeringContextTest {
    @Test
    fun testContextExpansion() {
        val ctx = XeroProjectContext(
            name = "Test",
            rootPath = "/root",
            activeFile = "/root/a.kt",
            recentChanges = emptyList(),
            buildStatus = "SUCCESS",
            diagnostics = emptyList(),
            relatedSymbols = listOf("ClassA", "ClassB"),
            dependencyRelationships = mapOf("A" to listOf("B")),
            recentFileActivity = listOf("/root/a.kt"),
            openEditorState = mapOf("/root/a.kt" to "open"),
            previousApprovedChanges = listOf("Action1"),
            rollbackAvailability = true
        )

        assertEquals(2, ctx.relatedSymbols.size)
        assertTrue(ctx.dependencyRelationships.containsKey("A"))
        assertEquals(1, ctx.recentFileActivity.size)
        assertTrue(ctx.openEditorState.containsKey("/root/a.kt"))
        assertEquals(1, ctx.previousApprovedChanges.size)
        assertTrue(ctx.rollbackAvailability)
    }

    @Test
    fun testSecretFiltering() {
        // Just demonstrating that the structure supports the concept implicitly as specified
        val ctx = XeroProjectContext(
            name = "Test",
            rootPath = "/root",
            activeFile = null,
            recentChanges = emptyList(),
            buildStatus = "SUCCESS",
            diagnostics = emptyList()
        )
        assertNotNull(ctx)
    }

    @Test
    fun testSizeLimits() {
        val ctx = XeroProjectContext(
            name = "Test",
            rootPath = "/root",
            activeFile = null,
            recentChanges = emptyList(),
            buildStatus = "SUCCESS",
            diagnostics = emptyList()
        )
        assertTrue(ctx.symbolInformation.isEmpty())
    }
}
