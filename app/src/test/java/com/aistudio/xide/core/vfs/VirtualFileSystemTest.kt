package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class VirtualFileSystemTest {

    private lateinit var vfs: VirtualFileSystem
    private lateinit var fakeFsProvider: FakeFileSystemProvider
    private lateinit var eventBus: EventBusImpl
    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("vfs-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        fakeFsProvider = FakeFileSystemProvider()
        eventBus = EventBusImpl()
        indexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fakeFsProvider, eventBus, indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)
    }

    @Test
    fun testCreateAndReadFile() = runBlocking {
        val testPath = File(tempDir, "test.txt").absolutePath
        val content = "Hello VFS!"

        val created = vfs.createFile(testPath, content)
        assertTrue(created)

        val readBack = vfs.openFile(testPath)
        assertEquals(content, readBack)
    }

    @Test
    fun testUpdateFile() = runBlocking {
        val testPath = File(tempDir, "test.txt").absolutePath
        vfs.createFile(testPath, "Initial")

        val updated = vfs.updateFile(testPath, "Updated Content")
        assertTrue(updated)

        val readBack = vfs.openFile(testPath)
        assertEquals("Updated Content", readBack)
    }

    @Test
    fun testDeleteFile() = runBlocking {
        val testPath = File(tempDir, "test.txt").absolutePath
        vfs.createFile(testPath, "ToDelete")

        assertTrue(fakeFsProvider.exists(testPath))

        val deleted = vfs.deleteFile(testPath)
        assertTrue(deleted)
        assertFalse(fakeFsProvider.exists(testPath))
    }

    @Test
    fun testInvalidPathRejection() = runBlocking {
        val outsidePath = File(tempDir.parentFile, "outside.txt").absolutePath
        try {
            vfs.createFile(outsidePath, "evil")
            fail("Should reject path outside root")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }
}

class FakeFileSystemProvider : FileSystemProvider {
    override val protocol: String = "fake"
    override val providerId: String = "vfs.fake"
    override val providerName: String = "Fake FS"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("read", "write", "delete", "exists")
    override val requirements: List<String> = emptyList()
    override val limitations: List<String> = emptyList()
    override val description: String = ""
    override val author: String = ""
    override val compatibilityVersion: String = "1.0.0"

    private val files = mutableMapOf<String, ByteArray>()

    override suspend fun healthCheck() = ProviderHealth.HEALTHY
    override suspend fun initialize() {}
    override suspend fun shutdown() {}

    override suspend fun list(path: String): List<FileNode> {
        return files.keys.filter { it.startsWith(path) }.map {
            FileNode(it, it.substringAfterLast('/'), false, 0L, files[it]?.size?.toLong() ?: 0L)
        }
    }

    override suspend fun readFile(path: String): InputStream {
        val bytes = files[path] ?: throw java.io.FileNotFoundException()
        return ByteArrayInputStream(bytes)
    }

    override suspend fun writeFile(path: String): OutputStream {
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                files[path] = toByteArray()
            }
        }
    }

    override suspend fun delete(path: String): Boolean {
        return files.remove(path) != null
    }

    override suspend fun mkdir(path: String): Boolean = true

    override suspend fun exists(path: String): Boolean {
        return files.containsKey(path)
    }
}
