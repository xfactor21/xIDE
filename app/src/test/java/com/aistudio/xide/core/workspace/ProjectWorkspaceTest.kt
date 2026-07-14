package com.aistudio.xide.core.workspace

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ProjectWorkspaceTest {

    @Test
    fun testCreateWorkspaceAndLoadWorkspace() {
        val metadata = ProjectMetadata(
            projectType = "android",
            buildSystem = "gradle",
            languages = listOf("Kotlin"),
            frameworks = listOf("Jetpack Compose"),
            dependencies = listOf("androidx.core:core-ktx")
        )
        
        val workspace = ProjectWorkspace(
            id = "test-ws-123",
            name = "TestProject",
            rootPath = "/tmp/test-project",
            metadata = metadata,
            activeFiles = listOf("MainActivity.kt"),
            state = WorkspaceState.IDLE
        )

        assertEquals("test-ws-123", workspace.id)
        assertEquals("TestProject", workspace.name)
        assertEquals("/tmp/test-project", workspace.rootPath)
        assertEquals("android", workspace.metadata.projectType)
        assertEquals("gradle", workspace.metadata.buildSystem)
        assertTrue(workspace.metadata.languages.contains("Kotlin"))
        assertEquals(WorkspaceState.IDLE, workspace.state)
    }

    @Test
    fun testValidatePaths() {
        val rootPath = File("/tmp/workspace-root").absolutePath
        val insidePath = File("/tmp/workspace-root/src/MainActivity.kt").absolutePath
        val outsidePath = File("/tmp/outside-root/MainActivity.kt").absolutePath

        val canonicalRoot = File(rootPath).canonicalPath
        val canonicalInside = File(insidePath).canonicalPath
        val canonicalOutside = File(outsidePath).canonicalPath

        assertTrue(canonicalInside.startsWith(canonicalRoot))
        assertFalse(canonicalOutside.startsWith(canonicalRoot))
    }
}
