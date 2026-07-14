package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.intelligence.analysis.LanguageDefinition
import com.aistudio.xide.core.intelligence.symbols.SymbolType
import com.aistudio.xide.core.intelligence.symbols.SymbolReference
import com.aistudio.xide.core.intelligence.symbols.SymbolLocation
import com.aistudio.xide.core.language.SyntaxAnalysis
import com.aistudio.xide.core.diagnostics.DiagnosticItem
import com.aistudio.xide.core.diagnostics.DiagnosticSource
import com.aistudio.xide.core.diagnostics.DiagnosticSeverity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntelligenceTests {

    @Test
    fun testLanguageDefinition() {
        val def = LanguageDefinition("kotlin", "Kotlin", listOf(".kt", ".kts"), listOf("syntax", "symbols"))
        assertEquals("kotlin", def.languageId)
        assertEquals("Kotlin", def.displayName)
    }

    @Test
    fun testSymbolReference() {
        val loc = SymbolLocation("/file.kt", 1, 0, 1, 10)
        val ref = SymbolReference("id1", "MyClass", SymbolType.CLASS, loc, "kotlin")
        assertEquals(SymbolType.CLASS, ref.type)
        assertEquals("/file.kt", ref.location.filePath)
    }

    @Test
    fun testSyntaxAnalysis() {
        val sa = SyntaxAnalysis(true, emptyList())
        assertEquals(true, sa.isReady)
        assertEquals(0, sa.tokens.size)
    }
    
    @Test
    fun testDiagnosticItem() {
        val item = DiagnosticItem("id1", "/file.kt", DiagnosticSeverity.ERROR, DiagnosticSource.COMPILER, "Syntax Error", 1, 1)
        assertEquals(DiagnosticSeverity.ERROR, item.severity)
        assertEquals(DiagnosticSource.COMPILER, item.source)
    }
}
