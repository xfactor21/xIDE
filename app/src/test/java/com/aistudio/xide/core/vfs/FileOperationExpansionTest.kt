package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.ProviderHealth
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.io.InputStream
import java.io.OutputStream

class FileOperationExpansionTest {
    class MockFs : FileSystemProvider {
        override val protocol = "mock"
        override val providerId = "mock"
        override val providerName = "mock"
        override val providerVersion = "1"
        override val supportedFeatures = emptyList<String>()
        override val requirements = emptyList<String>()
        override val limitations = emptyList<String>()
        override val description = "mock"
        override val author = "mock"
        override val compatibilityVersion = "1"
        
        var existsRet = true
        var moveRet = true
        var deleteRet = true

        override suspend fun list(path: String) = emptyList<FileNode>()
        override suspend fun readFile(path: String): InputStream = throw NotImplementedError()
        override suspend fun writeFile(path: String): OutputStream = throw NotImplementedError()
        override suspend fun delete(path: String) = deleteRet
        override suspend fun mkdir(path: String) = true
        override suspend fun exists(path: String) = existsRet
        override suspend fun move(sourcePath: String, targetPath: String) = moveRet
        
        override suspend fun healthCheck() = ProviderHealth.HEALTHY
        override suspend fun initialize() {}
        override suspend fun shutdown() {}
    }

    @Test
    fun testRename() = runBlocking {
        val fs = MockFs()
        val indexer = ProjectIndexerImpl()
        val vfs = VirtualFileSystem(fs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot("/root")
        
        val res = vfs.moveFile("/root/a.txt", "/root/b.txt")
        assertTrue(res)
    }

    @Test
    fun testMove() = runBlocking {
        val fs = MockFs()
        val indexer = ProjectIndexerImpl()
        val vfs = VirtualFileSystem(fs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot("/root")
        
        val res = vfs.moveFile("/root/src/a.txt", "/root/dst/a.txt")
        assertTrue(res)
    }

    @Test(expected = SecurityException::class)
    fun testSecurityRejection() = runBlocking {
        val fs = MockFs()
        val indexer = ProjectIndexerImpl()
        val vfs = VirtualFileSystem(fs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot("/root")
        
        vfs.moveFile("/root/a.txt", "/etc/passwd")
        Unit
    }
}
