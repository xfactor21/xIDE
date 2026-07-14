package com.aistudio.xide.core.indexing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ProjectIndexTest {

    private lateinit var indexer: ProjectIndexerImpl
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("index-test", "")
        tempDir.delete()
        tempDir.mkdirs()

        val srcDir = File(tempDir, "src").apply { mkdirs() }
        
        val mainFile = File(srcDir, "MainActivity.kt")
        mainFile.writeText("""
            package com.example.app
            import androidx.compose.runtime.Composable

            class MainActivity {
                fun onCreate() {
                }
            }
        """.trimIndent())

        val buildFile = File(tempDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.android.application") version "8.1.1"
                id("org.jetbrains.kotlin.android") version "1.9.0"
            }
            dependencies {
                implementation("androidx.core:core-ktx:1.10.1")
            }
        """.trimIndent())

        val manifestFile = File(tempDir, "AndroidManifest.xml")
        manifestFile.writeText("<manifest></manifest>")

        indexer = ProjectIndexerImpl()
    }

    @Test
    fun testFileDiscoveryAndSymbolDetection() = runBlocking {
        val task = indexer.indexProject(tempDir.absolutePath)
        task.await()

        val index = indexer.getIndex(tempDir.absolutePath)
        assertNotNull(index)
        
        assertTrue(index!!.files.any { it.endsWith("MainActivity.kt") })
        assertTrue(index.files.any { it.endsWith("build.gradle.kts") })

        val symbol = indexer.findClassDefinition(tempDir.absolutePath, "MainActivity")
        assertNotNull(symbol)
        assertEquals("Class", symbol?.type)
        assertEquals("Kotlin", symbol?.language)

        val context = indexer.getContext(tempDir.absolutePath)
        assertEquals("android", context.projectType)
        assertEquals("gradle", context.buildSystem)
        assertTrue(context.languages.contains("Kotlin"))
        assertTrue(context.dependencies.contains("androidx.core:core-ktx:1.10.1"))
    }
}
