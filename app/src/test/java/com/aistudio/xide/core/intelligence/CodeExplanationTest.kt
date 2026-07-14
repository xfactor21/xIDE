package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import com.aistudio.xide.core.vfs.VirtualFileSystem
import com.aistudio.xide.core.events.EventBusImpl
import com.aistudio.xide.core.provider.FileSystemProvider
import com.aistudio.xide.core.provider.FileNode
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.File

class CodeExplanationTest {

    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var vfs: VirtualFileSystem
    private lateinit var fakeFs: FakeFileSystemProvider
    private lateinit var engine: CodeIntelligenceEngine
    private lateinit var explanationService: CodeExplanationService
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("explain-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        fakeFs = FakeFileSystemProvider()
        indexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fakeFs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        engine = CodeIntelligenceEngineImpl(indexer, vfs, null, null)
        explanationService = CodeExplanationService(vfs, engine)
    }

    @Test
    fun testExplainFileAndClassAndError() = runBlocking {
        val testFile = File(tempDir, "Sample.kt")
        val content = """
            package com.example
            class Sample {
                fun execute() {}
            }
        """.trimIndent()

        testFile.writeText(content)
        val writeStream = fakeFs.writeFile(testFile.absolutePath)
        writeStream.write(content.toByteArray())
        writeStream.close()

        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        val fileExplanation = explanationService.explainFile(tempDir.absolutePath, testFile.absolutePath)
        assertNotNull(fileExplanation)
        assertTrue(fileExplanation.purpose.contains("Sample.kt"))
        assertTrue(fileExplanation.purpose.contains("Sample"))

        val classExplanation = explanationService.explainClass(tempDir.absolutePath, "Sample")
        assertNotNull(classExplanation)
        assertTrue(classExplanation.purpose.contains("Sample"))

        val errorExplanation = explanationService.explainError("Unresolved reference: buildService")
        assertNotNull(errorExplanation)
        assertTrue(errorExplanation.purpose.contains("Unresolved reference"))
        assertTrue(errorExplanation.dependencies.first().contains("Missing package import"))
    }

    private class FakeFileSystemProvider : FileSystemProvider {
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
}
