package com.aistudio.xide.core.workspace

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceArchitectureTest {

    @Test
    fun testWorkspaceInterfacesExist() {
        assertNotNull(Class.forName("com.aistudio.xide.core.workspace.WorkspaceManager"))
        assertNotNull(Class.forName("com.aistudio.xide.core.workspace.WorkspaceLifecycle"))
        assertNotNull(Class.forName("com.aistudio.xide.core.workspace.WorkspacePersistence"))
        assertNotNull(Class.forName("com.aistudio.xide.core.workspace.WorkspaceCache"))
        assertNotNull(Class.forName("com.aistudio.xide.core.workspace.WorkspaceRestore"))
    }
}
