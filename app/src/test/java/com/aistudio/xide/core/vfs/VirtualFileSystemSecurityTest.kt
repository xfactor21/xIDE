package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class VirtualFileSystemSecurityTest {

    private lateinit var vfs: VirtualFileSystem
    private lateinit var fakeFsProvider: FakeFileSystemProvider
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("vfs-security-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        fakeFsProvider = FakeFileSystemProvider()
        val eventBus = EventBusImpl()
        val indexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fakeFsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)
    }

    @Test
    fun testOutsideRootRejection() = runBlocking {
        val outsideFile = File(tempDir.parentFile, "malicious_outside_file.txt").absolutePath
        
        try {
            vfs.createFile(outsideFile, "should not work")
            fail("Should have thrown SecurityException for path outside root")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }

        try {
            vfs.openFile(outsideFile)
            fail("Should have thrown SecurityException for reading outside root")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun testTraversalAttackRejection() = runBlocking {
        // Constructing path-traversal relative path
        val traversalPath = tempDir.absolutePath + "/../malicious.txt"

        try {
            vfs.validatePath(traversalPath)
            fail("Should have rejected path traversal via canonical validation")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun testInvalidRootsAndEmptyValidation() = runBlocking {
        // Temporary clean-slate VFS without active root configured
        val cleanVfs = VirtualFileSystem(fakeFsProvider, EventBusImpl(), ProjectIndexerImpl())
        
        try {
            cleanVfs.validatePath("any_file.txt")
            fail("Should fail validation when no active workspace root is set")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("No active project workspace open") == true)
        }
    }

    @Test
    fun testNormalWorkspaceAccessIsAllowed() = runBlocking {
        val safeFile = File(tempDir, "safe_folder/nested.txt").absolutePath
        val canonicalSafe = vfs.validatePath(safeFile)
        
        assertEquals(File(safeFile).canonicalPath, canonicalSafe)
    }
}
