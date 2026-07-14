package com.aistudio.xide.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.Assert.*

class DiagnosticChainTest {

    class MockEngine : DiagnosticsEngine {
        private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
        override val activeDiagnostics: kotlinx.coroutines.flow.StateFlow<List<BuildDiagnostic>> = _activeDiagnostics

        override fun clearDiagnostics() {}
        override fun addDiagnostic(diagnostic: BuildDiagnostic) {}
        override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {}
        override fun getFormattedDiagnosticsForContext(): List<String> = emptyList()
        
        fun setActiveDiagnostics(list: List<BuildDiagnostic>) {
            _activeDiagnostics.value = list
        }
    }

    @Test
    fun testRootCauseGrouping() {
        val engine = MockEngine()
        engine.setActiveDiagnostics(listOf(
            BuildDiagnostic(
                message = "Unresolved reference: Player",
                severity = DiagnosticSeverity.ERROR,
                category = "compiler",
                compilerDiagnostic = null
            ),
            BuildDiagnostic(
                message = "Unresolved reference: Score",
                severity = DiagnosticSeverity.ERROR,
                category = "compiler",
                compilerDiagnostic = null
            )
        ))

        val analyzer = DiagnosticAnalyzer(engine)
        val chains = analyzer.buildDiagnosticChains()
        assertEquals(1, chains.size)
        assertEquals("Missing package or class import in target files.", chains[0].rootCause)
    }

    @Test
    fun testRelatedErrorAssociation() {
        val engine = MockEngine()
        engine.setActiveDiagnostics(listOf(
            BuildDiagnostic(
                message = "Unresolved reference: Player",
                severity = DiagnosticSeverity.ERROR,
                category = "compiler",
                compilerDiagnostic = null
            ),
            BuildDiagnostic(
                message = "Unresolved reference: Score",
                severity = DiagnosticSeverity.ERROR,
                category = "compiler",
                compilerDiagnostic = null
            )
        ))

        val analyzer = DiagnosticAnalyzer(engine)
        val chains = analyzer.buildDiagnosticChains()
        assertEquals(1, chains[0].relatedErrors.size)
        assertEquals("Unresolved reference: Score", chains[0].relatedErrors[0])
    }
}
