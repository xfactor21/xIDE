package com.aistudio.xide.core.intelligence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CodeNavigationIntegrationTest {
    @Test
    fun testNavigationFeatures() = runBlocking {
        val tempDir = File.createTempFile("nav-test", "")
        tempDir.delete()
        tempDir.mkdirs()
        
        val indexer = object : ProjectIndexer {
            override fun indexProject(projectPath: String): IndexingTask {
                return object : IndexingTask {
                    override val taskId = "test"
                    override val projectPath = projectPath
                    override val progress: StateFlow<Float> = MutableStateFlow(1f)
                    override val status: StateFlow<IndexingStatus> = MutableStateFlow(IndexingStatus.COMPLETED)
                    override suspend fun await() {}
                    override fun cancel() {}
                }
            }
            override suspend fun getIndex(projectPath: String): ProjectIndex {
                return ProjectIndex(
                    projectPath = projectPath,
                    files = listOf("/UserService.kt", "/UserRepository.kt", "/UserRepositoryImpl.kt"),
                    symbols = listOf(
                        ProjectSymbol("UserService", "Class", "/UserService.kt", "Kotlin", references = emptyList()),
                        ProjectSymbol("UserRepository", "Interface", "/UserRepository.kt", "Kotlin", references = emptyList()),
                        ProjectSymbol("UserRepositoryImpl", "Class", "/UserRepositoryImpl.kt", "Kotlin", references = listOf("UserRepository"))
                    ),
                    relationships = listOf(
                        ProjectRelationship("UserRepositoryImpl", "UserRepository", "extends"),
                        ProjectRelationship("UserService", "UserRepository", "imports")
                    ),
                    metadata = emptyMap()
                )
            }
            override suspend fun getContext(projectPath: String): ProjectContext {
                return ProjectContext(projectPath, emptyList(), "", "")
            }
        }
        
        val navigator = CodeNavigator(indexer)
        
        val defs = navigator.goToDefinition(tempDir.absolutePath, "UserRepository")
        assertEquals(1, defs.size)
        assertEquals("/UserRepository.kt", defs[0])
        
        val refs = navigator.findReferences(tempDir.absolutePath, "UserRepository")
        assertEquals(3, refs.size)
        
        val impls = navigator.findImplementations(tempDir.absolutePath, "UserRepository")
        assertEquals(1, impls.size)
        assertEquals("UserRepositoryImpl", impls[0])
        
        val hierarchy = navigator.getSymbolHierarchyView(tempDir.absolutePath, "UserRepository")
        assertTrue(hierarchy.contains("UserRepositoryImpl"))
    }
}
