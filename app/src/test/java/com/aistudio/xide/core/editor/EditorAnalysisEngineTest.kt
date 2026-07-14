package com.aistudio.xide.core.editor

import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorAnalysisEngineTest {
    @Test
    fun testLiveAnalysis() = runBlocking {
        val engine = EditorAnalysisEngine(null, null)
        val content = """
            import unused.Dependency
            
            class Test {
                fun test() {
                    // TODO: fix this
                }
            }
        """.trimIndent()
        
        engine.analyzeContent("/Test.kt", content)
        val diagnostics = engine.liveDiagnostics.value
        
        assertEquals(2, diagnostics.size)
        assertEquals("unused_import", diagnostics[0].type)
        assertEquals(DiagnosticSeverity.WARNING, diagnostics[0].severity)
        assertEquals(1, diagnostics[0].line)
        
        assertEquals("quality_hint", diagnostics[1].type)
        assertEquals(DiagnosticSeverity.INFO, diagnostics[1].severity)
        assertEquals(5, diagnostics[1].line)
    }
}
