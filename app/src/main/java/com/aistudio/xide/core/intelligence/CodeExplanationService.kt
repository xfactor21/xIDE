package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.vfs.VirtualFileSystem
import java.io.File

data class CodeExplanation(
    val purpose: String,
    val dependencies: List<String>,
    val sideEffects: List<String>,
    val relatedFiles: List<String>
)

class CodeExplanationService(
    private val vfs: VirtualFileSystem,
    private val codeIntelligenceEngine: CodeIntelligenceEngine
) {

    suspend fun explainFile(projectPath: String, filePath: String): CodeExplanation {
        val summary = codeIntelligenceEngine.analyzeSourceFile(projectPath, filePath)
        val shortName = File(filePath).name
        
        val purpose = "Source file '$shortName' written in Kotlin containing ${summary.symbols.size} code structures. " +
                "Its main defined element is ${summary.className ?: "unnamed component"}. " +
                "It serves as a module within the active project context."
        
        val dependencies = summary.imports.map { "Imported package/class: $it" }
        
        val sideEffects = summary.symbols.filter { it.type == "Function" }.map { 
            "Defines function '${it.name}' which may mutate state or execute operations when invoked."
        }

        val relationships = codeIntelligenceEngine.getSymbolRelationships(projectPath)
        val relatedFiles = relationships
            .filter { it.sourceSymbol == summary.className || it.targetSymbol == summary.className }
            .map { "Related through inheritance or import: ${if (it.sourceSymbol == summary.className) it.targetSymbol else it.sourceSymbol}" }
            .distinct()

        return CodeExplanation(
            purpose = purpose,
            dependencies = dependencies,
            sideEffects = sideEffects,
            relatedFiles = relatedFiles
        )
    }

    suspend fun explainClass(projectPath: String, className: String): CodeExplanation {
        val relationships = codeIntelligenceEngine.getSymbolRelationships(projectPath)
        
        val purpose = "Class '$className' serves as a modular object or logic provider in the architecture. " +
                "It is modeled with clean MVVM pattern boundaries."

        val dependencies = relationships
            .filter { it.sourceSymbol == className && it.relationshipType == "imports" }
            .map { it.targetSymbol }

        val sideEffects = listOf(
            "Lifecycle bounds are managed by Hilt constructors and service registries.",
            "Changes to '$className' properties propagate reactive Flow updates."
        )

        val relatedFiles = relationships
            .filter { it.sourceSymbol == className || it.targetSymbol == className }
            .map { if (it.sourceSymbol == className) it.targetSymbol else it.sourceSymbol }
            .distinct()

        return CodeExplanation(
            purpose = purpose,
            dependencies = dependencies,
            sideEffects = sideEffects,
            relatedFiles = relatedFiles
        )
    }

    suspend fun explainFunction(projectPath: String, functionName: String): CodeExplanation {
        return CodeExplanation(
            purpose = "Function '$functionName' executes specific modular sub-tasks, supporting state transitions and event flows.",
            dependencies = emptyList(),
            sideEffects = listOf("May throw exceptions if invoked before initialization.", "Can trigger state flow updates and trigger re-compositions in Compose."),
            relatedFiles = emptyList()
        )
    }

    suspend fun explainError(errorMessage: String): CodeExplanation {
        val cleanMsg = errorMessage.replace(Regex("\\[REDACTED_SECRET\\]"), "")
        val containsUnresolved = cleanMsg.contains("Unresolved reference", ignoreCase = true)
        
        val purpose = "Compiler issue diagnostic: '$cleanMsg'."
        val dependencies = if (containsUnresolved) listOf("Missing package import, mismatched dependency version, or missing build.gradle.kts reference.") else emptyList()
        val sideEffects = listOf("Blocks compiler from performing success checks.", "Causes incremental build execution failure.")
        val relatedFiles = emptyList<String>()

        return CodeExplanation(
            purpose = purpose,
            dependencies = dependencies,
            sideEffects = sideEffects,
            relatedFiles = relatedFiles
        )
    }

    suspend fun explainArchitectureRelationship(projectPath: String, source: String, target: String): CodeExplanation {
        return CodeExplanation(
            purpose = "Defines architectural relationship where '$source' interacts with, imports, or depends on '$target'.",
            dependencies = listOf("Direct compile dependency on '$target' api structures."),
            sideEffects = listOf("Mutations in '$target' might impact behavior or require refactoring of '$source'."),
            relatedFiles = listOf(source, target)
        )
    }
}
