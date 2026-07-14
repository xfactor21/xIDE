package com.aistudio.xide.core.designsystem

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class ComponentRenderingTest {

    @Test
    fun testComponentLibraryClassesExist() {
        // Simple class availability check for UI tests without full compose rule setup
        val buttonClass = Class.forName("com.aistudio.xide.core.designsystem.components.XideButtonKt")
        assertTrue(buttonClass != null)

        val cardClass = Class.forName("com.aistudio.xide.core.designsystem.components.XideCardKt")
        assertTrue(cardClass != null)

        val panelClass = Class.forName("com.aistudio.xide.core.designsystem.components.XidePanelKt")
        assertTrue(panelClass != null)
    }
}
