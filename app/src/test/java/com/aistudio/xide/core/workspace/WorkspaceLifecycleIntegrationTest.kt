package com.aistudio.xide.core.workspace

import com.aistudio.xide.core.ai.AiContextManagerImpl
import com.aistudio.xide.core.diagnostics.DiagnosticsEngineImpl
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.vfs.FileChangeTracker
import com.aistudio.xide.core.vfs.local.LocalFileSystemProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class WorkspaceLifecycleIntegrationTest {

    private lateinit var tempDir: File
    private lateinit var fsProvider: LocalFileSystemProvider
    private lateinit var eventBus: EventBusImpl
    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var vfs: VirtualFileSystem
    private lateinit var fileChangeTracker: FileChangeTracker
    private lateinit var diagnosticsEngine: DiagnosticsEngineImpl
    private lateinit var aiContextManager: AiContextManagerImpl

    @Before
    fun setUp() {
        tempDir = File.createTempFile("workspace-integration", "")
        tempDir.delete()
        tempDir.mkdirs()

        fsProvider = LocalFileSystemProvider()
        eventBus = EventBusImpl()
        indexer = ProjectIndexerImpl()
        
        vfs = VirtualFileSystem(fsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        diagnosticsEngine = DiagnosticsEngineImpl()
        aiContextManager = AiContextManagerImpl(
            projectIndexer = indexer,
            activeProjectRootProvider = { tempDir.absolutePath },
            diagnosticsEngine = diagnosticsEngine
        )

        fileChangeTracker = FileChangeTracker(
            eventBus = eventBus,
            diagnosticsEngine = diagnosticsEngine,
            buildService = null,
            aiContextManager = aiContextManager
        )
    }

    @Test
    fun testCompleteWorkspaceLifecycleFlow() = runBlocking {
        try {
            // 1. Setup a basic kotlin/gradle project structure
            val mainKtPath = File(tempDir, "MainActivity.kt").absolutePath
            val buildGradlePath = File(tempDir, "build.gradle.kts").absolutePath
            val manifestPath = File(tempDir, "AndroidManifest.xml").absolutePath

            vfs.createFile(mainKtPath, "package com.example\nclass MainActivity")
            vfs.createFile(buildGradlePath, "plugins { id(\"com.android.application\") version \"8.1.1\" }")
            vfs.createFile(manifestPath, "<manifest></manifest>")

            // 2. Index the project
            val indexingTask = indexer.indexProject(tempDir.absolutePath)
            indexingTask.await()

            val index = indexer.getIndex(tempDir.absolutePath)
            assertNotNull("Index should not be null", index)
            assertTrue("MainActivity should be in index", index!!.files.any { it.endsWith("MainActivity.kt") })

            // 3. Verify metadata extracted
            val contextBeforeChange = indexer.getContext(tempDir.absolutePath)
            assertEquals("android", contextBeforeChange.projectType)

            // 4. Update a file and track modifications
            vfs.updateFile(mainKtPath, "package com.example\nclass MainActivity\n// Modified")

            // Wait for async background subscriber to collect event
            kotlinx.coroutines.delay(300)

            // 5. Verify the file change was registered in the FileChangeTracker
            val safeMainKtPath = vfs.validatePath(mainKtPath)
            val modifiedFiles = fileChangeTracker.modifiedFiles
            assertTrue("fileChangeTracker.modifiedFiles ($modifiedFiles) should contain safeMainKtPath ($safeMainKtPath)", modifiedFiles.contains(safeMainKtPath))

            // 6. Refresh AI context snapshot
            val aiContextSnapshot = aiContextManager.captureCurrentContext()
            assertNotNull("aiContextSnapshot.projectContext should not be null", aiContextSnapshot.projectContext)
            assertEquals(tempDir.absolutePath, aiContextSnapshot.projectContext?.projectPath)
        } catch (t: Throwable) {
            val sw = java.io.StringWriter()
            t.printStackTrace(java.io.PrintWriter(sw))
            throw RuntimeException("DETAILED_FAILURE:\n" + sw.toString())
        }
    }
}
