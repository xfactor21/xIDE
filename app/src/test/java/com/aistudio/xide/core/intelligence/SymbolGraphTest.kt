package com.aistudio.xide.core.intelligence

import org.junit.Test
import org.junit.Assert.*

class SymbolGraphTest {
    @Test
    fun testRelationships() {
        val rel = ProjectRelationship("A", "B", "extends")
        assertEquals("A", rel.sourceSymbol)
        assertEquals("B", rel.targetSymbol)
        assertEquals("extends", rel.relationshipType)
    }

    @Test
    fun testReferences() {
        val sym = ProjectSymbol("MyClass", "Class", "file:1", "Kotlin")
        assertTrue(sym.references.isEmpty())
    }

    @Test
    fun testImplementations() {
        val sym = ProjectSymbol("MyInterface", "Interface", "file:1", "Kotlin", extendsList = listOf("Base"))
        assertTrue(sym.extendsList.contains("Base"))
    }
}
