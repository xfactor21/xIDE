package com.aistudio.xide.core.intelligence

import java.io.File

class CodeNavigator(private val projectIndexer: ProjectIndexer) {

    suspend fun goToDefinition(projectPath: String, symbolName: String): List<String> {
        val index = projectIndexer.getIndex(projectPath) ?: return emptyList()
        // Definitions are symbols whose name matches the symbol name exactly, with high priority for Class, Interface, Function
        return index.symbols
            .filter { it.name.equals(symbolName, ignoreCase = true) }
            .sortedBy { 
                when (it.type) {
                    "Class" -> 1
                    "Interface" -> 2
                    "Function" -> 3
                    else -> 4
                }
            }
            .map { it.location }
    }

    suspend fun findReferences(projectPath: String, symbolName: String): List<String> {
        val index = projectIndexer.getIndex(projectPath) ?: return emptyList()
        val references = mutableListOf<String>()

        // 1. Check direct import relationships
        index.relationships
            .filter { it.targetSymbol.equals(symbolName, ignoreCase = true) && it.relationshipType == "imports" }
            .forEach { references.add(it.sourceSymbol) }

        // 2. Check references field in ProjectSymbol
        index.symbols
            .filter { it.references.contains(symbolName) }
            .forEach { references.add(it.location) }

        // 3. Check extends/implements inheritance relationships
        index.relationships
            .filter { it.targetSymbol.equals(symbolName, ignoreCase = true) && it.relationshipType == "extends" }
            .forEach { references.add(it.sourceSymbol) }

        return references.distinct()
    }

    suspend fun symbolSearch(projectPath: String, query: String): List<ProjectSymbol> {
        val index = projectIndexer.getIndex(projectPath) ?: return emptyList()
        return index.symbols.filter { it.name.contains(query, ignoreCase = true) }
    }

    suspend fun lookupRelationships(projectPath: String, symbolName: String): List<ProjectRelationship> {
        val index = projectIndexer.getIndex(projectPath) ?: return emptyList()
        return index.relationships.filter { 
            it.sourceSymbol.equals(symbolName, ignoreCase = true) || it.targetSymbol.equals(symbolName, ignoreCase = true)
        }
    }
}
