package com.aistudio.xide.core.workspace

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceContainerTest {

    @Test
    fun testWorkspaceContainerExists() {
        val containerClass = Class.forName("com.aistudio.xide.core.workspace.WorkspaceContainer")
        assertTrue(containerClass.isInterface)
    }
}
