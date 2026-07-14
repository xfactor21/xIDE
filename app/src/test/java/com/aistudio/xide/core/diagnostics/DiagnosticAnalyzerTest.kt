package com.aistudio.xide.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DiagnosticAnalyzerTest {

    private lateinit var diagnosticsEngine: FakeDiagnosticsEngine
    private lateinit var analyzer: DiagnosticAnalyzer

    @Before
    fun setUp() {
        diagnosticsEngine = FakeDiagnosticsEngine()
        analyzer = DiagnosticAnalyzer(diagnosticsEngine)
    }

    @Test
    fun testDependencyAndUnresolvedGrouping() {
        // 1. Add dependency failure and cascading unresolved reference errors
        diagnosticsEngine.addDiagnostic(
            BuildDiagnostic(
                category = "dependency",
                severity = DiagnosticSeverity.ERROR,
                message = "Could not find library com.google.truth:truth:1.1.3",
                compilerDiagnostic = CompilerDiagnostic("MISSING_DEP", "Could not find library com.google.truth:truth:1.1.3", DiagnosticSeverity.ERROR, DiagnosticLocation("build.gradle.kts", 40, 5))
            )
        )
        diagnosticsEngine.addDiagnostic(
            BuildDiagnostic(
                category = "compiler",
                severity = DiagnosticSeverity.ERROR,
                message = "Unresolved reference: Truth",
                compilerDiagnostic = CompilerDiagnostic("UNRESOLVED_REFERENCE", "Unresolved reference: Truth", DiagnosticSeverity.ERROR, DiagnosticLocation("MyTest.kt", 10, 8))
            )
        )

        // 2. Perform analysis
        val groups = analyzer.analyzeDiagnostics()
        assertEquals(1, groups.size)

        val group = groups.first()
        assertTrue(group.primaryError.contains("Could not find library"))
        assertTrue(group.secondaryErrors.any { it.contains("Unresolved reference") })
        assertTrue(group.rootCauseCandidates.any { it.contains("Missing Gradle implementation") })
        assertTrue(group.suggestedActions.any { it.contains("Verify and insert missing library coordinates") })
    }

    private class FakeDiagnosticsEngine : DiagnosticsEngine {
        private val _activeDiagnostics = MutableStateFlow<List<BuildDiagnostic>>(emptyList())
        override val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = _activeDiagnostics.asStateFlow()

        override fun addDiagnostic(diagnostic: BuildDiagnostic) {
            _activeDiagnostics.value = _activeDiagnostics.value + diagnostic
        }

        override fun addDiagnostics(diagnostics: List<BuildDiagnostic>) {
            _activeDiagnostics.value = _activeDiagnostics.value + diagnostics
        }

        override fun clearDiagnostics() {
            _activeDiagnostics.value = emptyList()
        }

        override fun getFormattedDiagnosticsForContext(): List<String> = emptyList()
    }
}
