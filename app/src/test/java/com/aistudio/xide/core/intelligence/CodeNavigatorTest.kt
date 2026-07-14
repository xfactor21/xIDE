package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.indexing.ProjectIndexerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class CodeNavigatorTest {

    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var navigator: CodeNavigator
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("nav-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        val srcDir = File(tempDir, "src").apply { mkdirs() }
        
        val serviceFile = File(srcDir, "MyService.kt")
        serviceFile.writeText("""
            package com.example
            class MyService : MyInterface {
                fun perform() {
                }
            }
        """.trimIndent())

        val interfaceFile = File(srcDir, "MyInterface.kt")
        interfaceFile.writeText("""
            package com.example
            interface MyInterface {
            }
        """.trimIndent())

        indexer = ProjectIndexerImpl()
        navigator = CodeNavigator(indexer)
    }

    @Test
    fun testGoToDefinition() = runBlocking {
        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        val serviceDefs = navigator.goToDefinition(tempDir.absolutePath, "MyService")
        assertEquals(1, serviceDefs.size)
        assertTrue(serviceDefs.first().contains("MyService.kt"))

        val interfaceDefs = navigator.goToDefinition(tempDir.absolutePath, "MyInterface")
        assertEquals(1, interfaceDefs.size)
        assertTrue(interfaceDefs.first().contains("MyInterface.kt"))
    }

    @Test
    fun testFindReferencesAndSearch() = runBlocking {
        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        // 1. References of MyInterface (MyService implements/extends it)
        val refs = navigator.findReferences(tempDir.absolutePath, "MyInterface")
        assertTrue(refs.contains("MyService"))

        // 2. Symbol search
        val searchResults = navigator.symbolSearch(tempDir.absolutePath, "Service")
        assertEquals(1, searchResults.size)
        assertEquals("MyService", searchResults.first().name)
    }
}
