package com.aistudio.xide.core.diagnostics

data class DiagnosticGroup(
    val primaryError: String,
    val secondaryErrors: List<String>,
    val rootCauseCandidates: List<String>,
    val affectedFiles: List<String>,
    val suggestedActions: List<String>
)

data class DiagnosticChain(
    val rootCause: String,
    val relatedErrors: List<String>,
    val suggestedResolution: String
)

class DiagnosticAnalyzer(private val diagnosticsEngine: DiagnosticsEngine) {

    fun buildDiagnosticChains(): List<DiagnosticChain> {
        val groups = analyzeDiagnostics()
        return groups.map { group ->
            DiagnosticChain(
                rootCause = group.rootCauseCandidates.firstOrNull() ?: group.primaryError,
                relatedErrors = group.secondaryErrors,
                suggestedResolution = group.suggestedActions.joinToString("\n")
            )
        }
    }

    fun analyzeDiagnostics(): List<DiagnosticGroup> {
        val diagnostics = diagnosticsEngine.activeDiagnostics.value
        if (diagnostics.isEmpty()) return emptyList()

        val unresolvedReferences = diagnostics.filter { 
            it.message.contains("Unresolved reference", ignoreCase = true) || 
            it.compilerDiagnostic?.code == "UNRESOLVED_REFERENCE"
        }

        val dependencyIssues = diagnostics.filter { 
            it.category.equals("dependency", ignoreCase = true) || 
            it.message.contains("Could not find", ignoreCase = true) || 
            it.message.contains("Cannot resolve", ignoreCase = true)
        }

        val syntaxErrors = diagnostics.filter { 
            it.message.contains("Syntax error", ignoreCase = true) || 
            it.message.contains("Expecting", ignoreCase = true)
        }

        val groups = mutableListOf<DiagnosticGroup>()

        if (dependencyIssues.isNotEmpty()) {
            val primary = dependencyIssues.first().message
            val secondary = unresolvedReferences.map { it.message }
            val files = (dependencyIssues + unresolvedReferences)
                .mapNotNull { it.compilerDiagnostic?.location?.filePath }
                .distinct()

            groups.add(
                DiagnosticGroup(
                    primaryError = primary,
                    secondaryErrors = secondary,
                    rootCauseCandidates = listOf(
                        "Missing Gradle implementation or api dependency declarations.",
                        "Remote Maven dependency synchronization failed due to offline state.",
                        "Incompatible versions or mismatch in build.gradle.kts and libs.versions.toml."
                    ),
                    affectedFiles = files,
                    suggestedActions = listOf(
                        "Verify and insert missing library coordinates inside build.gradle.kts dependencies block.",
                        "Add required repositories like mavenCentral() or google() inside settings.gradle.kts.",
                        "Examine libs.versions.toml for typos or mismatched dot-notation definitions."
                    )
                )
            )
        } else if (unresolvedReferences.isNotEmpty()) {
            val primary = unresolvedReferences.first().message
            val secondary = unresolvedReferences.drop(1).map { it.message }
            val files = unresolvedReferences.mapNotNull { it.compilerDiagnostic?.location?.filePath }.distinct()

            groups.add(
                DiagnosticGroup(
                    primaryError = primary,
                    secondaryErrors = secondary,
                    rootCauseCandidates = listOf(
                        "Missing package or class import in target files.",
                        "The referenced class or method was recently renamed or deleted."
                    ),
                    affectedFiles = files,
                    suggestedActions = listOf(
                        "Add correct import statement at the top of the affected file.",
                        "Use CodeNavigator to find definition or verify package path naming correctness."
                    )
                )
            )
        }

        if (syntaxErrors.isNotEmpty()) {
            val primary = syntaxErrors.first().message
            val files = syntaxErrors.mapNotNull { it.compilerDiagnostic?.location?.filePath }.distinct()

            groups.add(
                DiagnosticGroup(
                    primaryError = primary,
                    secondaryErrors = syntaxErrors.drop(1).map { it.message },
                    rootCauseCandidates = listOf("Unmatched parentheses, missing brackets, or unexpected characters in Kotlin source files."),
                    affectedFiles = files,
                    suggestedActions = listOf("Examine code on the highlighted line and add missing brackets or remove stray punctuation.")
                )
            )
        }

        // Catch-all group if we couldn't match specific groupings
        if (groups.isEmpty()) {
            val primary = diagnostics.first().message
            val files = diagnostics.mapNotNull { it.compilerDiagnostic?.location?.filePath }.distinct()

            groups.add(
                DiagnosticGroup(
                    primaryError = primary,
                    secondaryErrors = diagnostics.drop(1).map { it.message },
                    rootCauseCandidates = listOf("General compiler diagnostic or project build environment configuration issue."),
                    affectedFiles = files,
                    suggestedActions = listOf("Review raw build output stacktrace or consult compile logs for underlying exception details.")
                )
            )
        }

        return groups
    }
}
