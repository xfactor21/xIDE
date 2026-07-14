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

class CodeIntelligenceTest {

    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var vfs: VirtualFileSystem
    private lateinit var fakeFs: FakeFileSystemProvider
    private lateinit var engine: CodeIntelligenceEngine
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("intel-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        fakeFs = FakeFileSystemProvider()
        indexer = ProjectIndexerImpl()
        vfs = VirtualFileSystem(fakeFs, EventBusImpl(), indexer)
        vfs.setActiveWorkspaceRoot(tempDir.absolutePath)

        engine = CodeIntelligenceEngineImpl(indexer, vfs, null, null)
    }

    @Test
    fun testSymbolExtractionAndRelationships() = runBlocking {
        val testFile = File(tempDir, "TestService.kt")
        val content = """
            package com.example.service
            
            import com.example.repo.MyRepository

            @Service
            internal class TestService : BaseService(), XideProvider {
                private val repo = MyRepository()
                
                constructor() {
                }

                override fun start() {
                }
            }
        """.trimIndent()

        // 1. Write the file into real filesystem and fake filesystem
        testFile.writeText(content)
        val writeStream = fakeFs.writeFile(testFile.absolutePath)
        writeStream.write(content.toByteArray())
        writeStream.close()

        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        val index = indexer.getIndex(tempDir.absolutePath)
        assertNotNull(index)

        // 2. Analyze source file via engine
        val summary = engine.analyzeSourceFile(tempDir.absolutePath, testFile.absolutePath)
        assertEquals(testFile.absolutePath, summary.filePath)
        assertEquals("TestService", summary.className)
        assertTrue(summary.imports.contains("com.example.repo.MyRepository"))

        // Check symbols extracted
        val symbols = summary.symbols
        val classSymbol = symbols.firstOrNull { it.type == "Class" }
        assertNotNull(classSymbol)
        assertEquals("TestService", classSymbol?.name)
        assertEquals("internal", classSymbol?.visibility)
        assertTrue(classSymbol?.isOverride == false)
        assertTrue(classSymbol?.annotations?.contains("@Service") == true)
        assertTrue(classSymbol?.extendsList?.contains("BaseService") == true)
        assertTrue(classSymbol?.extendsList?.contains("XideProvider") == true)

        val constructorSymbol = symbols.firstOrNull { it.type == "Constructor" }
        assertNotNull(constructorSymbol)

        val functionSymbol = symbols.firstOrNull { it.type == "Function" }
        assertNotNull(functionSymbol)
        assertEquals("start", functionSymbol?.name)
        assertTrue(functionSymbol?.isOverride == true)

        // Check relationships
        val relationships = engine.getSymbolRelationships(tempDir.absolutePath)
        assertTrue(relationships.any { it.sourceSymbol == "TestService" && it.targetSymbol == "BaseService" && it.relationshipType == "extends" })
        assertTrue(relationships.any { it.sourceSymbol == "TestService" && it.targetSymbol == "XideProvider" && it.relationshipType == "extends" })
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
