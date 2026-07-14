package com.aistudio.xide.core.intelligence

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectUnderstandingEngineTest {
    @Test
    fun testAnalyzeProject() = runBlocking {
        val testProvider = object : ProjectContextProvider {
            override suspend fun provideContext(projectPath: String, currentContext: ProjectContext): ProjectContext {
                return currentContext.copy(
                    projectType = "android_app",
                    buildSystem = "gradle"
                )
            }
        }
        val engine = DefaultProjectUnderstandingEngine(listOf(testProvider))
        val context = engine.analyzeProject("/project")
        
        assertEquals("/project", context.projectPath)
        assertEquals("android_app", context.projectType)
        assertEquals("gradle", context.buildSystem)
    }
}
