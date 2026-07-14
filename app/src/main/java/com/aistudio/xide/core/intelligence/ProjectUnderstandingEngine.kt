package com.aistudio.xide.core.intelligence

/**
 * High-level engine responsible for aggregating project knowledge from various
 * subsystems (Indexer, VFS, Diagnostics, BuildPipeline) into a unified ProjectContext.
 */
interface ProjectUnderstandingEngine {
    /**
     * Analyzes the project at the given path and returns a comprehensive context.
     * Extension Point: Plugins can register ProjectContextProviders to enrich this context.
     */
    suspend fun analyzeProject(projectPath: String): ProjectContext
}

/**
 * Extension point for plugins to provide specific facets of project understanding.
 */
interface ProjectContextProvider {
    suspend fun provideContext(projectPath: String, currentContext: ProjectContext): ProjectContext
}

/**
 * Production implementation that aggregates data from registered ProjectContextProviders.
 */
class DefaultProjectUnderstandingEngine(
    private val providers: List<ProjectContextProvider>
) : ProjectUnderstandingEngine {
    override suspend fun analyzeProject(projectPath: String): ProjectContext {
        // Base context without hardcoded metadata
        var context = ProjectContext(
            projectPath = projectPath,
            activeFiles = emptyList(),
            architectureSummary = "",
            contextPreparation = "",
            projectType = "unknown",
            languages = emptyList(),
            frameworks = emptyList(),
            buildSystem = "unknown",
            dependencies = emptyList(),
            activeProblems = emptyList(),
            recommendedActions = emptyList()
        )
        
        // Enrich context through provider chain
        for (provider in providers) {
            context = provider.provideContext(projectPath, context)
        }
        
        return context
    }
}
